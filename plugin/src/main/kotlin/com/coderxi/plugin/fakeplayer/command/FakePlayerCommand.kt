package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionMode
import com.coderxi.plugin.fakeplayer.api.action.ActionType
import com.coderxi.plugin.fakeplayer.api.action.FlattenAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.annotaion.HelpLine
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.command.annotaion.SuggestCommands
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler.CommandContext
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*
import com.coderxi.plugin.fakeplayer.component.FakePlayerDialog
import com.coderxi.plugin.fakeplayer.component.FakePlayerLimiter
import com.coderxi.plugin.fakeplayer.component.FakePlayerSelector.selected
import com.coderxi.plugin.fakeplayer.component.FlattenSelectionManager
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.*
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.help.Help
import revxrsal.commands.help.Help.RelatedCommands
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission as Permission

@Command("fakeplayer","fp")
class FakePlayerCommand {

    @Dependency lateinit var fpm: FakePlayerManager
    @Dependency lateinit var fpl: FakePlayerLimiter

    @Subcommand("help","?")
    @Permission(HELP, BASIC)
    @HelpLine("fakeplayer.help.cmd.help")
    fun CommandSender.help(
        @Range(min = 1.0) @Default("1") @Named("page") page: Int,
        relatedCommands: RelatedCommands<BukkitCommandActor?>
    ) {
        val pageSize = 10
        val commands =
            if (this is Player) relatedCommands.paginate(page, pageSize)
            else Help.paginate(relatedCommands.filter { !(it.annotations().get(HelpLine::class.java)?.playerOnly?:false) }, page, pageSize)
        val pageTotal = (relatedCommands.count() + pageSize - 1) / pageSize
        val lines = mutableListOf(
            tl("fakeplayer.help.header", page, pageTotal),
        )
        for (command in commands) {
            val anno = command.annotations().get(HelpLine::class.java) ?: continue
            listOf(anno, *anno.children).forEach { anno ->
                if (anno.descriptionKey.isEmpty()) return@forEach
                val usage = "/" + (anno.usage.ifEmpty { command.usage() })
                val desc = tls(anno.descriptionKey)
                val line = MiniMessage.miniMessage().deserialize(tls("fakeplayer.help.line", usage, desc))
                lines.add(line)
            }
        }
        lines.add(HelpLineUtil.paginationComponent(page,pageTotal))
        sendMessage(Component.join(JoinConfiguration.newlines(),lines))
    }

    @Subcommand("reload")
    @Permission(RELOAD,ADMIN)
    @HelpLine("fakeplayer.help.cmd.reload")
    fun CommandSender.reload() {
        plugin.onReload()
        sendMessage(tlp("fakeplayer.reload.success"))
    }

    @Subcommand("spawn")
    @Permission(SPAWN, BASIC)
    @HelpLine("fakeplayer.help.cmd.spawn", playerOnly = true)
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
    @HelpLine("fakeplayer.help.cmd.spawn-name")
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
    @HelpLine("fakeplayer.help.cmd.select")
    fun CommandSender.select(@Named("name") fakePlayer: FakePlayer) {
        selected = fakePlayer
        sendMessage(tlp("fakeplayer.select.success", fakePlayer.name))
    }

    @Subcommand("remove")
    @Permission(REMOVE,BASIC)
    @HelpLine("fakeplayer.help.cmd.remove", children = [
        HelpLine("fakeplayer.help.cmd.remove-all","fp remove --all")
    ])
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
    @HelpLine("fakeplayer.help.cmd.kill", children = [
        HelpLine("fakeplayer.help.cmd.kill-all","fp kill --all")
    ])
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
    @HelpLine("fakeplayer.help.cmd.invsee", playerOnly = true)
    fun Player.invsee(@Select fakePlayer: FakePlayer) {
        InvseeProvider.current.openInventory(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("enderchest")
    @HelpLine("fakeplayer.help.cmd.enderchest", playerOnly = true)
    @Permission(ENDER_CHEST,BASIC)
    fun Player.enderchest(@Select fakePlayer: FakePlayer) {
        InvseeProvider.current.openEnderChest(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("tp")
    @Permission(TP,BASIC)
    @HelpLine("fakeplayer.help.cmd.tp", playerOnly = true)
    fun Player.tp(@Select fakePlayer: FakePlayer) {
        teleportAsync(fakePlayer.player.location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tphere")
    @Permission(TP,BASIC)
    @HelpLine("fakeplayer.help.cmd.tphere", playerOnly = true)
    fun Player.tphere(@Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tpswap")
    @Permission(TP,BASIC)
    @HelpLine("fakeplayer.help.cmd.tpswap", playerOnly = true)
    fun Player.tpswap(@Select fakePlayer: FakePlayer) {
        val that = fakePlayer.player
        val thatLocation = that.location
        val thisLocation = this.location
        this.teleportAsync(thatLocation, Sound.ENTITY_ENDERMAN_TELEPORT)
        that.teleportAsync(thisLocation, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tppos")
    @Permission(TP,BASIC)
    @HelpLine("fakeplayer.help.cmd.tppos")
    fun CommandSender.tppos(@Named("location") location: Location, @Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("expme")
    @Permission(EXPME,BASIC)
    @HelpLine("fakeplayer.help.cmd.expme", playerOnly = true)
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
    @HelpLine("fakeplayer.help.cmd.skin")
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
    @HelpLine("fakeplayer.help.cmd.cmd")
    fun CommandSender.cmd(@Named("command") @SuggestCommands @Single command: String, @Select fakePlayer: FakePlayer) {
        Bukkit.dispatchCommand(fakePlayer.player, command.removePrefix("/"))
    }

    @Subcommand("chat")
    @Permission(CHAT,BASIC)
    @HelpLine("fakeplayer.help.cmd.chat")
    fun CommandSender.message(@Named("message") message: String, @Select fakePlayer: FakePlayer) {
        fakePlayer.nms.chat(message)
    }

    @Subcommand("settings")
    @Permission(SETTINGS,BASIC)
    @HelpLine("fakeplayer.help.cmd.settings", playerOnly = true)
    fun Player.settings(@Select fakePlayer: FakePlayer, context: CommandContext) {
        val player = this
        showDialog(FakePlayerDialog.settingsDialog(player, fakePlayer) { newName ->
            if (newName != null && newName != fakePlayer.name) {
                launch(context) {
                    val renamed = fpm.rename(fakePlayer, newName, player)
                    if (renamed != null) {
                        player.selected = renamed
                        sendMessage(tlp("fakeplayer.gui.settings.rename.success", renamed.name))
                    }
                }
            } else {
                sendMessage(tlp("fakeplayer.gui.settings.submit.success", fakePlayer.name))
                launch {
                    fpm.saveSettings(fakePlayer)
                }
            }
        })
    }

    @Subcommand("flatten")
    @Permission(FLATTEN, BASIC)
    @HelpLine("fakeplayer.help.cmd.flatten", children = [
        HelpLine("fakeplayer.help.cmd.flatten-cancel", "fp flatten cancel", playerOnly = true)
    ], playerOnly = true)
    fun Player.flatten() {
        val fpm = com.coderxi.plugin.fakeplayer.utils.plugin.fakePlayerManager
        val targetFp = this.selected ?: fpm.fakeplayersByOwnerUuid(uniqueId).firstOrNull() ?: fpm.fakeplayers().firstOrNull()
        showDialog(FakePlayerDialog.flattenDialog(this, targetFp))
    }

    @Subcommand("flatten")
    @Permission(FLATTEN, BASIC)
    fun Player.flatten(@Named("name") fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.flattenDialog(this, fakePlayer))
    }

    @Subcommand("flatten cancel")
    @Permission(FLATTEN, BASIC)
    fun Player.flattenCancel() {
        FlattenSelectionManager.cancelSelection(this)
        sendMessage(tlp("fakeplayer.flatten.cancel"))
    }

    @Subcommand("owner", "owner list")
    @Permission(OWNER_LIST,BASIC)
    @HelpLine("", children = [
        HelpLine("fakeplayer.help.cmd.owner-list", "fp owner list [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.owner-add", "fp owner add [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.owner-remove", "fp owner remove [name]", playerOnly = true)
    ])
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
    @HelpLine("fakeplayer.help.cmd.import")
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
    @HelpLine("fakeplayer.help.cmd.action", children = [
        HelpLine("fakeplayer.help.cmd.action-start", "fp action start <action> [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.action-execute", "fp action execute <action> [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.action-stopall", "fp action stopall [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.action-stop", "fp action stop <action> [name]", playerOnly = true),
    ])
    fun Player.actionListUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionListDialog(this,fakePlayer))
    }

    @Subcommand("action start")
    @Permission(ACTION, BASIC)
    fun Player.actionUI(type: ActionType, @Select fakePlayer: FakePlayer) {
        assertPermission("${ACTION.value}.${type.name.lowercase()}", BASIC)
        showDialog(FakePlayerDialog.actionExecuteDialog(this, fakePlayer, type))
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
