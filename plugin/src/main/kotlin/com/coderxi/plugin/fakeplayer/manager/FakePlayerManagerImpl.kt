package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerPreparingEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerSpawnedEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.command.permission.Permission
import com.coderxi.plugin.fakeplayer.config.PreventKickingType
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.coderxi.plugin.fakeplayer.repository.FakePlayerRepository
import com.coderxi.plugin.fakeplayer.utils.*
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerKickEvent
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class FakePlayerManagerImpl : FakePlayerManager, Listener {

    companion object {
        const val MAX_NAME_LENGTH: Int = 16
        const val MIN_NAME_LENGTH: Int = 3
    }

    val repository = FakePlayerRepository()
    val registry = FakePlayerRegistry()
    override fun fakeplayers() = registry.sortedFakeplayers
    override fun fakeplayersCount() = registry.fakeplayers.count()
    override fun fakeplayersByOwners(): Map<UUID, Collection<UUID>> = registry.fakeplayersByOwnerUuids

    override fun fakeplayersByOwnerUuid(ownerUuid: UUID) = registry.fakeplayersByOwnerUuid(ownerUuid)
    override fun get(uuid: UUID): FakePlayer? = registry.fakeplayers[uuid]
    override fun get(name: String): FakePlayer? = registry.fakeplayersByName[name.lowercase()]

    override suspend fun getFromRepository(uuid: UUID): FakePlayer? = withContext(Dispatchers.IO) { repository.findByUuid(uuid) }
    override suspend fun getFromRepository(name: String): FakePlayer? = withContext(Dispatchers.IO) { repository.findByName(name) }

    private fun uuid(name: String) = UUID.nameUUIDFromBytes("${plugin.name}:$name".toByteArray())

    private val pendingSpawn = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).build<UUID, Boolean>()

    override suspend fun spawn(name: String, spawner: CommandSender, location: Location?) : FakePlayer {
        val spawnerAsPlayer = spawner as? Player
        val spawnerName = spawner.name
        val spawnerUuid = spawner.uniqueId()
        val spawnerIp = spawnerAsPlayer?.address?.address?.hostAddress ?: "127.0.0.1"
        val spawnLocation = location ?: spawnerAsPlayer?.location ?: plugin.server.worlds.first().spawnLocation
        val fakePlayer = withContext(Dispatchers.IO) {
            repository.findByName(name)
        } ?: StandardFakePlayer(name, uuid(name),spawnerUuid, mutableSetOf(spawnerUuid),null, plugin.config.defaultSettings.clone()).also {
            withContext(Dispatchers.IO) { repository.save(it, true) }
        }
        if (pendingSpawn.getIfPresent(fakePlayer.uuid) == true) {
            throw SpawnDuplicateSpawningException(fakePlayer.name)
        }
        pendingSpawn.put(fakePlayer.uuid,true)
        val address = IPGenerator.next()
        AsyncPlayerPreLoginEvent(name,address, fakePlayer.uuid, false).let { event ->
            event.callEvent()
            if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                pendingSpawn.invalidate(fakePlayer.uuid)
                throw SpawnDisallowedException(event.kickMessage())
            }
        }
        withContext(spawnLocation.dispatcher) {
            fakePlayer.spawnerName = spawnerName
            fakePlayer.spawnerUuid = spawnerUuid
            fakePlayer.spawnerIp = spawnerIp
            fakePlayer.spawnTime = System.currentTimeMillis()
            FakePlayerPreparingEvent(fakePlayer).callEvent()
            val nmsPlayer = plugin.nmsServer.newPlayer(fakePlayer.uuid, fakePlayer.name, spawnLocation).apply {
                disableAdvancements()
                setupClientOptions()
                fakePlayer.skin?.let {
                    setTextures(it.textures, it.signature)
                } ?: run {
                    setupDefaultSkin(spawner)
                }
            }
            val nmsConnection = plugin.nmsServer.placeNewPlayer(nmsPlayer.player, address)
            registry.register(fakePlayer)
            fakePlayer.onConnected(nmsPlayer, nmsConnection)
            FakePlayerConnectedEvent(fakePlayer).callEvent()
        }
        val spawned = fakePlayer.player.teleportAsync(spawnLocation).await()
        withContext(fakePlayer.dispatcher) {
            if (spawned) {
                fakePlayer.ticking = true
                FakePlayerSpawnedEvent(fakePlayer).callEvent()
                delay(1000)
                pendingSpawn.invalidate(fakePlayer.uuid)
            }
            else {
                pendingSpawn.invalidate(fakePlayer.uuid)
                fakePlayer.quit("Spawn failed")
            }
        }
        return fakePlayer
    }

    override suspend fun rename(fakePlayer: FakePlayer, newName: String, operator: CommandSender): FakePlayer? {
        val player = operator as? Player
        if (!plugin.config.name.pattern.matches(newName)) throw SpawnNameInvalidException(newName)
        if (get(newName) != null) throw SpawnAlreadyExistsException(newName)
        if (player != null && isNameUsed(newName)) {
            val fakePlayerInRepo = getFromRepository(newName)
            if (fakePlayerInRepo != null && fakePlayerInRepo.ownerUuids.isNotEmpty() && !fakePlayerInRepo.ownerUuids.contains(player.uniqueId) && !player.hasPermission(Permission.ADMIN.value)) {
                throw SpawnNameAlreadyUsedException(newName)
            }
        }

        val oldUuid = fakePlayer.uuid
        val newUuid = uuid(newName)
        val location = fakePlayer.player.location.clone()
        val skin = fakePlayer.skin
        val settings = fakePlayer.settings.copy()
        val creatorUuid = fakePlayer.creatorUuid
        val ownerUuids = fakePlayer.ownerUuids.toMutableSet()
        val oldPlayer = fakePlayer.player

        // 完整拷貝舊假人的所有背包物品、裝備、副手、終界箱、血量、飢餓度、經驗與藥水效果
        val invContents = oldPlayer.inventory.contents.map { it?.clone() }.toTypedArray()
        val armorContents = oldPlayer.inventory.armorContents.map { it?.clone() }.toTypedArray()
        val extraContents = oldPlayer.inventory.extraContents.map { it?.clone() }.toTypedArray()
        val enderChestContents = oldPlayer.enderChest.contents.map { it?.clone() }.toTypedArray()
        val health = oldPlayer.health
        val foodLevel = oldPlayer.foodLevel
        val exp = oldPlayer.exp
        val level = oldPlayer.level
        val totalExperience = oldPlayer.totalExperience
        val gameMode = oldPlayer.gameMode
        val potionEffects = oldPlayer.activePotionEffects.toList()

        // 1. 強制保存舊假人資料至磁碟並退出
        withContext(fakePlayer.dispatcher) {
            oldPlayer.saveData()
            fakePlayer.quit("Renamed to $newName")
        }

        // 2. 遷移 SQLite 數據與磁碟上的 playerdata / stats / advancements
        val newFakePlayer = StandardFakePlayer(newName, newUuid, creatorUuid, ownerUuids, skin, settings)
        withContext(Dispatchers.IO) {
            repository.rename(oldUuid, newFakePlayer)

            val worldContainer = Bukkit.getWorldContainer()
            val worlds = Bukkit.getWorlds()
            val targetFolders = mutableSetOf<File>()
            worlds.forEach { targetFolders.add(it.worldFolder) }
            targetFolders.add(File(worldContainer, "world"))

            for (folder in targetFolders) {
                val oldDat = File(folder, "playerdata/$oldUuid.dat")
                if (oldDat.exists()) {
                    val newDat = File(folder, "playerdata/$newUuid.dat")
                    oldDat.copyTo(newDat, overwrite = true)
                    oldDat.delete()
                }
                val oldDatOld = File(folder, "playerdata/$oldUuid.dat_old")
                if (oldDatOld.exists()) {
                    val newDatOld = File(folder, "playerdata/$newUuid.dat_old")
                    oldDatOld.copyTo(newDatOld, overwrite = true)
                    oldDatOld.delete()
                }
                val oldStats = File(folder, "stats/$oldUuid.json")
                if (oldStats.exists()) {
                    val newStats = File(folder, "stats/$newUuid.json")
                    oldStats.copyTo(newStats, overwrite = true)
                    oldStats.delete()
                }
                val oldAdv = File(folder, "advancements/$oldUuid.json")
                if (oldAdv.exists()) {
                    val newAdv = File(folder, "advancements/$newUuid.json")
                    oldAdv.copyTo(newAdv, overwrite = true)
                    oldAdv.delete()
                }
            }
        }

        // 3. 原地生成新假人
        delay(200)
        val spawnedFakePlayer = spawn(newName, operator, location)

        // 4. 將舊假人的背包物資與設定完整注入新假人
        withContext(spawnedFakePlayer.dispatcher) {
            spawnedFakePlayer.settings = settings.copy()
            val newPlayer = spawnedFakePlayer.player
            newPlayer.inventory.contents = invContents
            newPlayer.inventory.armorContents = armorContents
            newPlayer.inventory.extraContents = extraContents
            newPlayer.enderChest.contents = enderChestContents
            newPlayer.health = health.coerceAtMost(newPlayer.maxHealth)
            newPlayer.foodLevel = foodLevel
            newPlayer.exp = exp
            newPlayer.level = level
            newPlayer.totalExperience = totalExperience
            newPlayer.gameMode = gameMode
            potionEffects.forEach { newPlayer.addPotionEffect(it) }
            newPlayer.saveData()
        }

        withContext(Dispatchers.IO) {
            repository.saveSettings(spawnedFakePlayer)
        }

        return spawnedFakePlayer
    }

    private suspend fun NMSServerPlayer.setupDefaultSkin(spawner: CommandSender) {
        val defaultSkin = plugin.config.skin.default
        if (defaultSkin.isBlank() || defaultSkin == "NONE") {
            return
        } else if (defaultSkin == "SPAWNER") {
            if (spawner is Player) copyTextures(spawner)
        } else {
            SkinFetcher.getPlayerSkinInfoByName(defaultSkin.split(',').random(), true)?.let { randomSkin ->
                setTextures(randomSkin.textures, randomSkin.signature)
            }
        }
    }

    override suspend fun sequenceName(spawner: Player, reservedSequenceLength: Int): String {
        val spawnerNameWithSequenceNamePrefix = plugin.config.name.sequenceNamePrefix + spawner.name
        val maxBaseLength = MAX_NAME_LENGTH - 1 - reservedSequenceLength
        val safeBaseLength = maxBaseLength.coerceAtLeast(MIN_NAME_LENGTH)
        val baseName = if (spawnerNameWithSequenceNamePrefix.length > safeBaseLength)  spawnerNameWithSequenceNamePrefix.take(safeBaseLength) else spawnerNameWithSequenceNamePrefix
        val regex = Regex("^" + Regex.escape(baseName) + "_(\\d+)$")
        val existingSequences = fakeplayersByOwnerUuid(spawner.uniqueId)
            .map(FakePlayer::name)
            .mapNotNull { regex.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            .toSet()
        var number = 1
        val maxNumber = if (!spawner.hasPermission(Permission.ADMIN.value)) 10.0.pow(reservedSequenceLength.toDouble()).toInt() - 1 else plugin.config.limit.serverSpawn
        while (number < maxNumber) {
            if (number in existingSequences) {
                number++
                continue
            }
            val checkName = "${baseName}_$number"
            val fakePlayer = withContext(Dispatchers.IO) {
                repository.findByName(checkName)
            }
            if (fakePlayer == null || fakePlayer.ownerUuids.contains(spawner.uniqueId)) {
                return checkName
            } else if (fakePlayer.creatorUuid == null || fakePlayer.ownerUuids.isEmpty()) {
                //方便老数据迁移,如果通过序号召唤出来的假人有数据但无所有者,将第一个召唤者变为所有者
                fakePlayer.creatorUuid = spawner.uniqueId
                fakePlayer.ownerUuids.add(spawner.uniqueId)
                withContext(Dispatchers.IO) {
                    repository.save(fakePlayer, true)
                }
                return checkName
            }
            number++
        }
        throw SpawnNoAvailableSequenceNameException()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFakePlayerKick(event: PlayerKickEvent) {
        if (plugin.config.behavior.preventKicking == PreventKickingType.SPAWNING && pendingSpawn.getIfPresent(event.player.uniqueId) != null) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun unregisterOnQuit(event: FakePlayerQuitedEvent) {
        registry.unregister(event.fakePlayer.uuid)
    }

    override fun isNameUsed(name: String): Boolean {
        // 在线的有name 直接返回使用过
        val onlineCache = Bukkit.getPlayerExact(name)
        if (onlineCache != null) return true
        // 服务器有name缓存 直接返回未使用过
        val offlineCache = Bukkit.getOfflinePlayerIfCached(name)
        return offlineCache != null && offlineCache.hasPlayedBefore()
    }

    override fun isFake(uuid: UUID, queryFromRepository: Boolean): Boolean {
        val onlineResult = get(uuid) != null
        return if (!queryFromRepository) {
            onlineResult
        } else {
            onlineResult && repository.findByUuid(uuid) != null
        }
    }

    override suspend fun saveSkin(fakePlayer: FakePlayer) {
        withContext(Dispatchers.IO) {repository.saveSkin(fakePlayer)}
    }

    override suspend fun saveSettings(fakePlayer: FakePlayer) {
        withContext(Dispatchers.IO) {repository.saveSettings(fakePlayer)}
    }

    override suspend fun addOwner(fakePlayer: FakePlayer, ownerUuid: UUID) {
        fakePlayer.ownerUuids.add(ownerUuid)
        registry.register(fakePlayer)
        withContext(Dispatchers.IO) {repository.save(fakePlayer, true)}
    }

    override suspend fun removeOwner(fakePlayer: FakePlayer, ownerUuid: UUID) {
        fakePlayer.ownerUuids.remove(ownerUuid)
        registry.register(fakePlayer)
        withContext(Dispatchers.IO) {repository.save(fakePlayer, true)}
    }

    override suspend fun importFakePlayerData(databaseFile: File, tableName: String): Int {
        return repository.importFakePlayerData(databaseFile, tableName)
    }

}