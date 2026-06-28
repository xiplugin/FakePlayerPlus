package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerPreparingEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerSpawnedEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.SpawnNoAvailableSequenceNameException
import com.coderxi.plugin.fakeplayer.command.permission.Permission
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.coderxi.plugin.fakeplayer.repository.FakePlayerRepository
import com.coderxi.plugin.fakeplayer.utils.BukkitMain
import com.coderxi.plugin.fakeplayer.utils.EMPTY_UUID
import com.coderxi.plugin.fakeplayer.utils.IPGenerator
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import com.coderxi.plugin.fakeplayer.utils.SkinFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.io.File
import java.util.*
import kotlin.math.pow

class FakePlayerManagerImpl : FakePlayerManager, PluginComponent, Listener {

    companion object {
        const val MAX_NAME_LENGTH: Int = 16
        const val MIN_NAME_LENGTH: Int = 3
    }

    val repository = FakePlayerRepository()
    val registry = FakePlayerRegistry()
    override fun fakeplayers() = registry.fakeplayers.values
    override fun fakeplayersCount() = registry.fakeplayers.count()
    override fun fakeplayersByOwners(): Map<UUID, Collection<UUID>> = registry.fakeplayersByOwnerUuids

    override fun fakeplayersByOwnerUuid(ownerUuid: UUID) = registry.fakeplayersByOwnerUuid(ownerUuid)
    override fun get(uuid: UUID): FakePlayer? = registry.fakeplayers[uuid]
    override fun get(name: String): FakePlayer? = registry.fakeplayersByName[name]

    override suspend fun getFromRepository(uuid: UUID): FakePlayer? = withContext(Dispatchers.IO) { repository.findByUuid(uuid) }
    override suspend fun getFromRepository(name: String): FakePlayer? = withContext(Dispatchers.IO) { repository.findByName(name) }

    private fun uuid(name: String) = UUID.nameUUIDFromBytes("${plugin.name}:$name".toByteArray())

    override suspend fun spawn(name: String, spawner: CommandSender, location: Location?) : FakePlayer? {
        val spawnerAsPlayer = spawner as? Player
        val spawnerName = spawnerAsPlayer?.name ?: "system"
        val spawnerUuid = spawnerAsPlayer?.uniqueId ?: EMPTY_UUID
        val spawnerIp = spawnerAsPlayer?.address?.address?.hostAddress ?: "127.0.0.1"
        val spawnLocation = location ?: spawnerAsPlayer?.location ?: plugin.server.worlds.first().spawnLocation
        val fakePlayer = withContext(Dispatchers.IO) {
            repository.findByName(name)
        } ?: StandardFakePlayer(name, uuid(name),spawnerUuid, mutableSetOf(spawnerUuid),null, plugin.config.defaultSettings.clone()).also {
            withContext(Dispatchers.IO) { repository.save(it, true) }
        }
        withContext(Dispatchers.BukkitMain) {
            fakePlayer.spawnerName = spawnerName
            fakePlayer.spawnerUuid = spawnerUuid
            fakePlayer.spawnerIp = spawnerIp
            FakePlayerPreparingEvent(fakePlayer).callEvent()
            val nmsPlayer =  plugin.nmsServer.newPlayer(fakePlayer.uuid, fakePlayer.name).apply {
                disableAdvancements()
                setupClientOptions()
                fakePlayer.skin?.let {
                    setTextures(it.textures, it.signature)
                } ?: run {
                    setupDefaultSkin(spawner)
                }
            }
            registry.register(fakePlayer)
            val nmsNetwork = plugin.nms.createNetwork(IPGenerator.next())
            val nmsConnection = nmsNetwork.placeNewPlayer(nmsPlayer.player)
            fakePlayer.onConnected(nmsPlayer, nmsConnection)
            FakePlayerConnectedEvent(fakePlayer).callEvent()
        }
        val spawned = fakePlayer.player.teleportAsync(spawnLocation).await()
        withContext(Dispatchers.BukkitMain) {
            if (spawned) FakePlayerSpawnedEvent(fakePlayer).callEvent()
            else fakePlayer.quit("Spawn failed")
        }
        return fakePlayer
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
        val maxBaseLength = MAX_NAME_LENGTH - 1 - reservedSequenceLength
        val safeBaseLength = maxBaseLength.coerceAtLeast(MIN_NAME_LENGTH)
        val baseName = if (spawner.name.length > safeBaseLength)  spawner.name.take(safeBaseLength) else spawner.name
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