package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionMode
import com.coderxi.plugin.fakeplayer.api.action.ActionType
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
import com.coderxi.plugin.fakeplayer.utils.assertPermission
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.teleportAsync
import com.coderxi.plugin.fakeplayer.utils.tl
import com.coderxi.plugin.fakeplayer.utils.tlp
import com.coderxi.plugin.fakeplayer.utils.tls
import com.coderxi.plugin.fakeplayer.utils.uniqueId
import kotlinx.coroutines.Dispatchers
import com.coderxi.plugin.fakeplayer.utils.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.minimessage.MiniMessage
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

    private data class HelpEntry(
        val usage: String,
        val descriptionKey: String,
        val permission: com.coderxi.plugin.fakeplayer.command.permission.Permission = BASIC,
        val playerOnly: Boolean = false
    )

    private val helpEntries = listOf(
        HelpEntry("/fp help [page]", "fakeplayer.help.cmd.help", HELP),
        HelpEntry("/fp spawn", "fakeplayer.help.cmd.spawn", SPAWN, playerOnly = true),
        HelpEntry("/fp spawn <name>", "fakeplayer.help.cmd.spawn-name", SPAWN_WITH_NAME),
        HelpEntry("/fp select <name>", "fakeplayer.help.cmd.select", SELECT),
        HelpEntry("/fp remove [name] [--all]", "fakeplayer.help.cmd.remove", REMOVE),
        HelpEntry("/fp kill [name] [--all]", "fakeplayer.help.cmd.kill", KILL),
        HelpEntry("/fp invsee [name]", "fakeplayer.help.cmd.invsee", INVSEE, playerOnly = true),
        HelpEntry("/fp enderchest [name]", "fakeplayer.help.cmd.enderchest", ENDER_CHEST, playerOnly = true),
        HelpEntry("/fp tp [name]", "fakeplayer.help.cmd.tp", TP, playerOnly = true),
        HelpEntry("/fp tphere [name]", "fakeplayer.help.cmd.tphere", TP, playerOnly = true),
        HelpEntry("/fp tpswap [name]", "fakeplayer.help.cmd.tpswap", TP, playerOnly = true),
        HelpEntry("/fp tppos <location> [name]", "fakeplayer.help.cmd.tppos", TP),
        HelpEntry("/fp skin <name> [name]", "fakeplayer.help.cmd.skin", SKIN),
        HelpEntry("/fp cmd <command> [name]", "fakeplayer.help.cmd.cmd", CMD),
        HelpEntry("/fp chat <message> [name]", "fakeplayer.help.cmd.chat", CHAT),
        HelpEntry("/fp settings [name]", "fakeplayer.help.cmd.settings", SETTINGS, playerOnly = true),
        HelpEntry("/fp action [name]", "fakeplayer.help.cmd.action", ACTION, playerOnly = true),
        HelpEntry("/fp action start <action> [name]", "fakeplayer.help.cmd.action-start", ACTION, playerOnly = true),
        HelpEntry("/fp action execute <action> <mode> [name]", "fakeplayer.help.cmd.action-execute", ACTION),
        HelpEntry("/fp action stop <action> [name]", "fakeplayer.help.cmd.action-stop", ACTION),
        HelpEntry("/fp action stopall [name]", "fakeplayer.help.cmd.action-stopall", ACTION),
        HelpEntry("/fp owner list [name]", "fakeplayer.help.cmd.owner-list", OWNER_LIST, playerOnly = true),
        HelpEntry("/fp owner add <player> [name]", "fakeplayer.help.cmd.owner-add", OWNER_ADD, playerOnly = true),
        HelpEntry("/fp owner remove <player> [name]", "fakeplayer.help.cmd.owner-remove", OWNER_REMOVE, playerOnly = true),
        HelpEntry("/fp reload", "fakeplayer.help.cmd.reload", RELOAD),
        HelpEntry("/fp import <database> <table>", "fakeplayer.help.cmd.import", ADMIN)
    )

    private fun CommandSender.showHelp(page: Int) {
        val isPlayer = this is Player
        val availableEntries = helpEntries.filter { entry ->
            if (entry.playerOnly && !isPlayer) return@filter false
            if (entry.permission == ADMIN) {
                hasPermission(ADMIN)
            } else if (entry.permission == SPAWN_WITH_NAME) {
                hasPermission(SPAWN_WITH_NAME, ADMIN)
            } else {
                hasPermission(entry.permission, BASIC)
            }
        }

        val pageSize = 7
        val totalPages = (availableEntries.size + pageSize - 1) / pageSize
        val targetPage = if (totalPages == 0) 1 else page.coerceIn(1, totalPages)

        if (page < 1 || (totalPages > 0 && page > totalPages)) {
            sendMessage(tlp("fakeplayer.help.page-invalid", totalPages))
            return
        }

        sendMessage(tl("fakeplayer.help.header", targetPage, totalPages))
        val startIndex = (targetPage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(availableEntries.size)

        for (i in startIndex until endIndex) {
            val entry = availableEntries[i]
            val desc = tls(entry.descriptionKey)
            val hoverText = tls("fakeplayer.help.suggest-hover", entry.usage)
            val line = MiniMessage.miniMessage().deserialize(
                "<click:suggest_command:'${entry.usage}'><hover:show_text:'<gray>$hoverText</gray>'><gold>${entry.usage}</gold></hover></click> <dark_gray>-</dark_gray> <white>$desc</white>"
            )
            sendMessage(line)
        }

        if (targetPage < totalPages) {
            sendMessage(tl("fakeplayer.help.footer", targetPage + 1))
        }
    }

    @Subcommand("help")
    @Permission(HELP, BASIC)
    fun CommandSender.help(@Default("1") @Named("page") page: Int) {
        showHelp(page)
    }

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
    fun CommandSender.remove(@Select fakePlayer: FakePlayer) {
        fpm.get(fakePlayer.name)?.quit("Removed by $name")
        sendMessage(tlp("fakeplayer.remove.success", fakePlayer.name))
        fakePlayer.owners.forEach {
            if (it.uniqueId!=uniqueId()) it.sendMessage(tlp("fakeplayer.remove.success.with-operator", name, fakePlayer.name))
        }
    }

    @Subcommand("remove --all")
    @Permission(REMOVE,BASIC)
    fun CommandSender.removeAll() {
        fpm.fakeplayersByOwnerUuid(uniqueId()).forEach { fakePlayer ->
            remove(fakePlayer)
        }
    }

    @Subcommand("kill")
    @Permission(KILL,BASIC)
    fun CommandSender.kill(@Select fakePlayer: FakePlayer) {
        fakePlayer.player.health = 0.0
    }

    @Subcommand("kill --all")
    @Permission(KILL,BASIC)
    fun CommandSender.killAll() {
        fpm.fakeplayersByOwnerUuid(uniqueId()).forEach { kill(it) }
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
    fun CommandSender.tppos(location: Location, @Select fakePlayer: FakePlayer) {
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
    fun CommandSender.skin(@Named("name") targetName: String, @Select fakePlayer: FakePlayer) {
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
    fun CommandSender.cmd(@Named("command") @SuggestCommands @Single command: String, @Select fakePlayer: FakePlayer) {
        Bukkit.dispatchCommand(fakePlayer.player, command.removePrefix("/"))
    }

    @Subcommand("chat")
    @Permission(CHAT,BASIC)
    fun CommandSender.message(@Named("message") message: String, @Select fakePlayer: FakePlayer) {
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
    fun CommandSender.importFakePlayerData(@Named("database") databaseName: String, @Named("table") tableName: String, context: CommandContext) {
        val databaseFile = File(plugin.dataFolder, databaseName)
        if (!databaseFile.exists()) throw MissingDatabaseFileException(databaseName)
        launch(context) {
            val result = fpm.importFakePlayerData(databaseFile, tableName)
            sendMessage(tlp("fakeplayer.database.import-data.success", result))
        }
    }

    @Subcommand("action")
    @Permission(ACTION, BASIC)
    fun Player.actionListUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionListDialog(this,fakePlayer))
    }

    @Subcommand("action start")
    @Permission(ACTION, BASIC)
    fun Player.actionUI(type: ActionType, @Select fakePlayer: FakePlayer) {
        assertPermission("${ACTION.value}.${type.name.lowercase()}", BASIC)
        showDialog(FakePlayerDialog.actionExecuteDialog(fakePlayer, type))
    }

    @Subcommand("action execute")
    @Permission(ACTION, BASIC)
    fun CommandSender.executeAction(type: ActionType, mode: ActionMode, @Select fakePlayer: FakePlayer) {
        assertPermission("${ACTION.value}.${type.name.lowercase()}", BASIC)
        fakePlayer.actions.dispatch(Action.toClass(type).getConstructor(mode.javaClass).newInstance(mode))
    }

    @Subcommand("action stopall")
    @Permission(ACTION, BASIC)
    fun stopAllAction(@Select fakePlayer: FakePlayer) {
        fakePlayer.actions.stopAll()
    }

    @Subcommand("action stop")
    @Permission(ACTION, BASIC)
    fun stopAction(type: ActionType, @Select fakePlayer: FakePlayer) {
        fakePlayer.actions.stop(type)
    }

}
