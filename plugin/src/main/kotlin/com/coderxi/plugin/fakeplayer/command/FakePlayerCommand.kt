package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.command.annotaion.SuggestCommands
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler.CommandContext
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*
import com.coderxi.plugin.fakeplayer.component.FakePlayerDialog
import com.coderxi.plugin.fakeplayer.component.FakePlayerLimiter
import com.coderxi.plugin.fakeplayer.component.FakePlayerSelector.selected
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.utils.dispatcher
import com.coderxi.plugin.fakeplayer.utils.SkinFetcher
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.teleportAsync
import com.coderxi.plugin.fakeplayer.utils.tlp
import kotlinx.coroutines.Dispatchers
import com.coderxi.plugin.fakeplayer.utils.launch
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission as Permission

@Command("fakeplayer","fp")
class FakePlayerCommand {

    @Dependency lateinit var fpm: FakePlayerManager
    @Dependency lateinit var fpl: FakePlayerLimiter

    @Subcommand("reload")
    @Permission(RELOAD,ADMIN)
    fun CommandSender.reload() {
        plugin.onReload()
        sendMessage(tlp("fakeplayer.reload.success"))
    }

    @Subcommand("spawn")
    @Permission(SPAWN, BASIC)
    fun Player.spawn(context: CommandContext) {
        val player = this
        assertNoSpawnLimited()
        launch(context) {
            val name = fpm.sequenceName(player, ceil((fpl.getPlayerSpawnLimit(player)/10.0)).toInt())
            executeSpawn(name)
        }
    }


    @Subcommand("spawn")
    @Permission(SPAWN_WITH_NAME, ADMIN)
    fun CommandSender.spawn(@Named("name") name: String, context: CommandContext) {
        val player = this as? Player
        if (!plugin.config.name.pattern.matches(name)) throw SpawnNameInvalidException(name)
        assertNoSpawnLimited()
        launch(context) {
            if (fpm.get(name) != null) throw SpawnAlreadyExistsException(name)
            if (player != null && fpm.isNameUsed(name)) {
                val fakePlayer = fpm.getFromRepository(name)
                if (fakePlayer != null && fakePlayer.ownerUuids.isNotEmpty() && !fakePlayer.ownerUuids.contains(player.uniqueId) && !player.hasPermission(ADMIN)) {
                    throw SpawnNameAlreadyUsedException(name)
                }
            }
            executeSpawn(name)
        }
    }

    fun CommandSender.assertNoSpawnLimited() {
        if (this !is Player) return
        if (hasPermission(ADMIN.value)) return
        if (fpl.isServerLimited()) throw SpawnServerLimitedException()
        if (fpl.isPlayerLimited(this)) throw SpawnPlayerLimitedException()
        if (fpl.isIpLimited(this)) throw SpawnIpLimitedException()
        if (fpl.isTpsAdaptiveLimited(this)) throw SpawnTpsAdaptiveLimitedException()
    }

    suspend fun CommandSender.executeSpawn(name: String) {
        val fakePlayer = fpm.spawn(name, this) ?: throw SpawnUnknownException()
        val locationText = "%.2f, %.2f, %.2f".format(fakePlayer.nms.x, fakePlayer.nms.y, fakePlayer.nms.z)
        sendMessage(tlp("fakeplayer.spawn.success", name, fakePlayer.player.world.name, locationText))
        selected = fakePlayer
        withContext(fakePlayer.dispatcher) {
            fakePlayer.player.apply { world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f) }
        }
    }

    @Subcommand("select")
    @Permission(SELECT,BASIC)
    fun CommandSender.select(fakePlayer: FakePlayer) {
        selected = fakePlayer
        sendMessage(tlp("fakeplayer.select.success", fakePlayer.name))
    }

    @Subcommand("remove")
    @Permission(REMOVE,BASIC)
    fun Player.remove(@Select fakePlayer: FakePlayer) {
        fpm.get(fakePlayer.name)?.quit("Removed by $name")
        sendMessage(tlp("fakeplayer.remove.success", fakePlayer.name))
        fakePlayer.owners.forEach {
            if (it.uniqueId!=uniqueId) it.sendMessage(tlp("fakeplayer.remove.success.with-operator", name, fakePlayer.name))
        }
    }

    @Subcommand("remove --all")
    @Permission(REMOVE,BASIC)
    fun Player.removeAll() {
        fpm.fakeplayersByOwnerUuid(uniqueId).forEach { fakePlayer ->
            remove(fakePlayer)
        }
    }

    @Subcommand("kill")
    @Permission(KILL,BASIC)
    fun Player.kill(@Select fakePlayer: FakePlayer) {
        fakePlayer.player.health = 0.0
    }

    @Subcommand("kill --all")
    @Permission(KILL,BASIC)
    fun Player.killAll() {
        fpm.fakeplayersByOwnerUuid(uniqueId).forEach { kill(it) }
    }

    @Subcommand("invsee")
    @Permission(INVSEE,BASIC)
    fun Player.invsee(@Select fakePlayer: FakePlayer) {
        InvseeProvider.current.openInventory(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("enderchest")
    @Permission(ENDER_CHEST,BASIC)
    fun Player.enderchest(@Select fakePlayer: FakePlayer) {
        InvseeProvider.current.openEnderChest(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("tp")
    @Permission(TP,BASIC)
    fun Player.tp(@Select fakePlayer: FakePlayer) {
        teleportAsync(fakePlayer.player.location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tphere")
    @Permission(TP,BASIC)
    fun Player.tphere(@Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tpswap")
    @Permission(TP,BASIC)
    fun Player.tpswap(@Select fakePlayer: FakePlayer) {
        val that = fakePlayer.player
        val thatLocation = that.location
        val thisLocation = this.location
        this.teleportAsync(thatLocation, Sound.ENTITY_ENDERMAN_TELEPORT)
        that.teleportAsync(thisLocation, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tppos")
    @Permission(TP,BASIC)
    fun Player.tppos(location: Location, @Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("expme")
    @Permission(EXPME,BASIC)
    fun Player.expme(@Select fakePlayer: FakePlayer) {
        val totalExp = fakePlayer.player.calculateTotalExperiencePoints()
        if (totalExp == 0) throw HasNoMoreExperience(fakePlayer.name)
        fakePlayer.player.level = 0
        fakePlayer.player.exp = 0f
        giveExp(totalExp, false)
        playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
    }

    @Subcommand("skin")
    @Permission(SKIN,BASIC)
    @Cooldown(value = 1, unit = TimeUnit.MINUTES)
    fun Player.skin(@Named("name") targetName: String, @Select fakePlayer: FakePlayer) {
        launch {
            val skin = SkinFetcher.getPlayerSkinInfoByName(targetName)
            withContext(fakePlayer.dispatcher) {
                fakePlayer.skin = skin
                fakePlayer.player.world.playSound(fakePlayer.player.location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
            }
            fpm.saveSkin(fakePlayer)
        }
    }

    @Subcommand("cmd")
    @Permission(CMD,BASIC)
    fun Player.cmd(@Named("command") @SuggestCommands @Single command: String, @Select fakePlayer: FakePlayer) {
        Bukkit.dispatchCommand(fakePlayer.player, command.removePrefix("/"))
    }

    @Subcommand("chat")
    @Permission(CHAT,BASIC)
    fun Player.message(@Named("message") message: String, @Select fakePlayer: FakePlayer) {
        fakePlayer.nms.chat(message)
    }

    @Subcommand("settings")
    @Permission(SETTINGS,BASIC)
    fun Player.settings(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.settingsDialog(fakePlayer) {
            sendMessage(tlp("fakeplayer.gui.settings.submit.success", fakePlayer.name))
            launch {
                fpm.saveSettings(fakePlayer)
            }
        })
    }

    @Subcommand("owner list")
    @Permission(OWNER_LIST,BASIC)
    fun Player.ownerList(@Select fakePlayer: FakePlayer) {
        if (fakePlayer.ownerUuids.size == 1 && fakePlayer.ownerUuids.contains(uniqueId)) {
            sendMessage(tlp("fakeplayer.owner.list",fakePlayer.name,name))
            return
        }
        launch {
            val names = withContext(Dispatchers.IO) { fakePlayer.ownerUuids.mapNotNull { Bukkit.getOfflinePlayer(it).name } }
            sendMessage(tlp("fakeplayer.owner.list",fakePlayer.name,names.joinToString(", ")))
        }
    }

    @Subcommand("owner add")
    @Permission(OWNER_ADD,BASIC)
    fun Player.addOwner(@Named("player") owner: Player, @Select fakePlayer: FakePlayer) {
        if (fpm.get(owner.uniqueId)!= null) throw OwnerMustBeHumanException(owner.name, fakePlayer.name)
        if (fakePlayer.ownerUuids.contains(owner.uniqueId)) throw OwnerAlreadyBoundException(owner.name ,fakePlayer.name)
        launch {
            fpm.addOwner(fakePlayer,owner.uniqueId)
            sendMessage(tlp("fakeplayer.owner.add.success", owner.name,fakePlayer.name))
        }
    }

    @Subcommand("owner remove")
    @Permission(OWNER_REMOVE,BASIC)
    fun Player.removeOwner(@Named("player") owner: Player, @Select fakePlayer: FakePlayer) {
        if (owner.uniqueId == fakePlayer.creatorUuid) throw OwnerIsCreatorCannotBeRemovedException(owner.name ,fakePlayer.name)
        if (!fakePlayer.ownerUuids.contains(owner.uniqueId)) throw OwnerNotBoundCannotBeRemovedException(owner.name ,fakePlayer.name)
        launch {
            fpm.removeOwner(fakePlayer,owner.uniqueId)
            sendMessage(tlp("fakeplayer.owner.remove.success", owner.name,fakePlayer.name))
        }
    }

    @Subcommand("import")
    @Permission(ADMIN)
    fun Player.importFakePlayerData(@Named("database") databaseName: String, @Named("table") tableName: String, context: CommandContext) {
        val databaseFile = File(plugin.dataFolder, databaseName)
        if (!databaseFile.exists()) throw MissingDatabaseFileException(databaseName)
        launch(context) {
            val result = fpm.importFakePlayerData(databaseFile, tableName)
            sendMessage(tlp("fakeplayer.database.import-data.success", result))
        }
    }

}