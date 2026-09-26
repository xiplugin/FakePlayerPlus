package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.annotaion.HelpLine
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.command.annotaion.SuggestCommands
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler.CommandContext
import com.coderxi.plugin.fakeplayer.command.parameter.ActionModeAndParameters
import com.coderxi.plugin.fakeplayer.command.parameter.FakePlayerParameterType.DefaultSuggestions as SuggestOwnedFakePlayers
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*
import com.coderxi.plugin.fakeplayer.command.permission.hasPermission
import com.coderxi.plugin.fakeplayer.component.FakePlayerLimiter
import com.coderxi.plugin.fakeplayer.component.FakePlayerSelector.selected
import com.coderxi.plugin.fakeplayer.dialog.FakePlayerActionExecuteDialog
import com.coderxi.plugin.fakeplayer.dialog.FakePlayerActionListDialog
import com.coderxi.plugin.fakeplayer.dialog.FakePlayerSettingsDialog
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.utils.bukkit.SkinFetcher
import com.coderxi.plugin.fakeplayer.utils.bukkit.assertPermission
import com.coderxi.plugin.fakeplayer.utils.bukkit.teleportAsync
import com.coderxi.plugin.fakeplayer.utils.bukkit.uniqueIdOrZero
import com.coderxi.plugin.fakeplayer.utils.coroutine.dispatcher
import com.coderxi.plugin.fakeplayer.utils.coroutine.launch
import com.coderxi.plugin.fakeplayer.utils.messages.*
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
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
class FakePlayerCommand : PluginComponent {

    val fpl get() = FakePlayerLimiter

    @Subcommand("help","?")
    @Permission(HELP)
    @HelpLine("fakeplayer.help.cmd.help")
    fun CommandSender.help(
        @Range(min = 1.0) @Default("1") @Named("page") page: Int,
        relatedCommands: RelatedCommands<BukkitCommandActor?>
    ) {
        val locale = localOrDefault()
        val pageSize = 10
        val commands =
            if (this is Player) relatedCommands.paginate(page, pageSize)
            else Help.paginate(relatedCommands.filter { !(it.annotations().get(HelpLine::class.java)?.playerOnly?:false) }, page, pageSize)
        val pageTotal = (relatedCommands.count() + pageSize - 1) / pageSize
        val lines = mutableListOf(
            tl(locale,"fakeplayer.help.header", page, pageTotal),
        )
        for (command in commands) {
            val anno = command.annotations().get(HelpLine::class.java) ?: continue
            listOf(anno, *anno.children).forEach { anno ->
                if (anno.descriptionKey.isEmpty()) return@forEach
                val usage = "/" + (anno.usage.ifEmpty { command.usage() })
                val desc = tls(locale,anno.descriptionKey)
                val line = tl(locale,"fakeplayer.help.line", usage, desc)
                lines.add(line)
            }
        }
        lines.add(MessageBuilder.pagination(locale,page,pageTotal, "/fp help"))
        sendMessage(Component.join(JoinConfiguration.newlines(),lines))
    }

    @Subcommand("reload")
    @Permission(RELOAD)
    @HelpLine("fakeplayer.help.cmd.reload")
    fun CommandSender.reload() {
        plugin.onReload()
        sendLocalizedMessage("fakeplayer.reload.success")
    }

    @Subcommand("spawn")
    @Permission(SPAWN)
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
    @Permission(SPAWN_WITH_NAME)
    @HelpLine("fakeplayer.help.cmd.spawn-name")
    fun CommandSender.spawn(@Named("name") name: String, context: CommandContext) {
        val player = this as? Player
        if (!plugin.config.name.pattern.matches(name)) throw SpawnNameInvalidException(name)
        assertNoSpawnLimited()
        launch(context) {
            if (fpm.get(name) != null) throw SpawnAlreadyExistsException(name)
            if (player != null && fpm.isNameUsed(name)) {
                val fakePlayer = fpm.getFromRepository(name)
                if (fakePlayer != null && fakePlayer.hasOwner && !fakePlayer.isOwnedBy(player.uniqueId) && !player.hasPermission(ADMIN)) {
                    throw SpawnNameAlreadyUsedException(name)
                }
            }
            executeSpawn(name)
        }
    }

    @Subcommand("rename")
    @Permission(SPAWN_WITH_NAME)
    @HelpLine("fakeplayer.help.cmd.rename")
    fun CommandSender.rename(@SuggestWith(SuggestOwnedFakePlayers::class) @Named("name") oldName: String, @Named("newName") newName: String, @Switch("force") force: Boolean = false, context: CommandContext) {
        val operator = this
        if (!hasPermission(ADMIN) && force) throw NoPermissionException()
        launch(context) {
            val renamed = fpm.rename(oldName, newName, operator, force)
            sendLocalizedMessage("fakeplayer.rename.success", oldName, renamed.name)
            selected = renamed
        }
    }

    fun CommandSender.assertNoSpawnLimited() {
        if (this !is Player) return
        if (hasPermission(ADMIN)) return
        if (fpl.isServerLimited()) throw SpawnServerLimitedException()
        if (fpl.isPlayerLimited(this)) throw SpawnPlayerLimitedException()
        if (fpl.isIpLimited(this)) throw SpawnIpLimitedException()
        if (fpl.isTpsAdaptiveLimited(this)) throw SpawnTpsAdaptiveLimitedException()
    }

    suspend fun CommandSender.executeSpawn(name: String) {
        val fakePlayer = fpm.spawn(name, this) ?: throw SpawnUnknownException()
        val locationText = "%.2f, %.2f, %.2f".format(fakePlayer.nms.x, fakePlayer.nms.y, fakePlayer.nms.z)
        sendLocalizedMessage("fakeplayer.spawn.success", name, fakePlayer.player.world.name, locationText)
        selected = fakePlayer
        withContext(fakePlayer.dispatcher) {
            fakePlayer.player.apply { world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f) }
        }
    }

    @Subcommand("select")
    @Permission(SELECT)
    @HelpLine("fakeplayer.help.cmd.select")
    fun CommandSender.select(@Named("name") fakePlayer: FakePlayer) {
        selected = fakePlayer
        sendLocalizedMessage("fakeplayer.select.success", fakePlayer.name)
    }

    @Subcommand("remove")
    @Permission(REMOVE)
    @HelpLine("fakeplayer.help.cmd.remove", children = [
        HelpLine("fakeplayer.help.cmd.remove-all","fp remove --all")
    ])
    fun CommandSender.remove(@Select fakePlayer: FakePlayer) {
        fpm.get(fakePlayer.name)?.quit("Removed by $name")
        sendLocalizedMessage("fakeplayer.remove.success", fakePlayer.name)
        fakePlayer.owners.forEach {
            if (it.uuid!=uniqueIdOrZero) Bukkit.getPlayer(it.uuid)?.sendLocalizedMessage("fakeplayer.remove.success.with-operator", name, fakePlayer.name)
        }
    }

    @Subcommand("remove --all")
    @Permission(REMOVE)
    fun CommandSender.removeAll() {
        fpm.fakeplayersByOwnerUuid(uniqueIdOrZero).forEach { fakePlayer ->
            remove(fakePlayer)
        }
    }

    @Subcommand("kill")
    @Permission(KILL)
    @HelpLine("fakeplayer.help.cmd.kill", children = [
        HelpLine("fakeplayer.help.cmd.kill-all","fp kill --all")
    ])
    fun CommandSender.kill(@Select fakePlayer: FakePlayer) {
        fakePlayer.player.health = 0.0
    }

    @Subcommand("kill --all")
    @Permission(KILL)
    fun CommandSender.killAll() {
        fpm.fakeplayersByOwnerUuid(uniqueIdOrZero).forEach { kill(it) }
    }

    @Subcommand("respawn")
    @Permission(RESPAWN)
    @HelpLine("fakeplayer.help.cmd.respawn")
    fun CommandSender.respawn(@Select fakePlayer: FakePlayer) {
        if (fakePlayer.player.isDead) {
            fakePlayer.nms.respawn()
            if (this is Player) {
                fakePlayer.player.teleportAsync(location, Sound.ENTITY_ENDERMAN_TELEPORT)
            }
        }
    }

    @Subcommand("invsee")
    @Permission(INVSEE)
    @HelpLine("fakeplayer.help.cmd.invsee", playerOnly = true)
    fun Player.invsee(@Select fakePlayer: FakePlayer) {
        InvseeProvider.openInventory(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("enderchest")
    @HelpLine("fakeplayer.help.cmd.enderchest", playerOnly = true)
    @Permission(ENDER_CHEST)
    fun Player.enderchest(@Select fakePlayer: FakePlayer) {
        InvseeProvider.openEnderChest(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("tp")
    @Permission(TP)
    @HelpLine("fakeplayer.help.cmd.tp", playerOnly = true)
    fun Player.tp(@Select fakePlayer: FakePlayer) {
        teleportAsync(fakePlayer.player.location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tphere")
    @Permission(TP)
    @HelpLine("fakeplayer.help.cmd.tphere", playerOnly = true)
    fun Player.tphere(@Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tpswap")
    @Permission(TP)
    @HelpLine("fakeplayer.help.cmd.tpswap", playerOnly = true)
    fun Player.tpswap(@Select fakePlayer: FakePlayer) {
        val that = fakePlayer.player
        val thatLocation = that.location
        val thisLocation = this.location
        this.teleportAsync(thatLocation, Sound.ENTITY_ENDERMAN_TELEPORT)
        that.teleportAsync(thisLocation, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("tppos")
    @Permission(TP)
    @HelpLine("fakeplayer.help.cmd.tppos")
    fun CommandSender.tppos(@Named("location") location: Location, @Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location, Sound.ENTITY_ENDERMAN_TELEPORT)
    }

    @Subcommand("expme")
    @Permission(EXPME)
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
    @Permission(SKIN)
    @Cooldown(value = 1, unit = TimeUnit.MINUTES)
    @HelpLine("fakeplayer.help.cmd.skin")
    fun CommandSender.skin(@Named("name") targetName: String, @Select fakePlayer: FakePlayer) {
        launch {
            val skin = SkinFetcher.getPlayerTexturesByName(targetName)
            withContext(fakePlayer.dispatcher) {
                fakePlayer.textures = skin
                fakePlayer.player.world.playSound(fakePlayer.player.location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
            }
            fpm.saveSkin(fakePlayer)
        }
    }

    @Subcommand("cmd")
    @Permission(CMD)
    @HelpLine("fakeplayer.help.cmd.cmd")
    fun CommandSender.cmd(@Named("command") @SuggestCommands @Single command: String, @Select fakePlayer: FakePlayer) {
        Bukkit.dispatchCommand(fakePlayer.player, command.removePrefix("/"))
    }

    @Subcommand("chat")
    @Permission(CHAT)
    @HelpLine("fakeplayer.help.cmd.chat")
    fun CommandSender.message(@Named("message") message: String, @Select fakePlayer: FakePlayer) {
        fakePlayer.nms.chat(message)
    }

    @Subcommand("swap")
    @Permission(SWAP)
    @HelpLine("fakeplayer.help.cmd.swap")
    fun swapHandItem(@Select fakePlayer: FakePlayer) {
        fakePlayer.nms.swapHandItem()
    }

    @Subcommand("settings")
    @Permission(SETTINGS)
    @HelpLine("fakeplayer.help.cmd.settings", playerOnly = true)
    fun Player.settings(@Select fakePlayer: FakePlayer) {
        FakePlayerSettingsDialog(fakePlayer, this).show(this)
    }

    @Subcommand("owner", "owner list")
    @Permission(OWNER_LIST)
    @HelpLine("", children = [
        HelpLine("fakeplayer.help.cmd.owner-list", "fp owner list [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.owner-add", "fp owner add [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.owner-remove", "fp owner remove [name]", playerOnly = true)
    ])
    fun Player.ownerList(@Select fakePlayer: FakePlayer) {
        if (fakePlayer.owners.size == 1 && fakePlayer.isOwnedBy(uniqueId)) {
            sendLocalizedMessage("fakeplayer.owner.list",fakePlayer.name,name)
            return
        }
        val names = fakePlayer.owners.map { it.name }
        sendLocalizedMessage("fakeplayer.owner.list",fakePlayer.name,names.joinToString(", "))
    }

    @Subcommand("owner add")
    @Permission(OWNER_ADD)
    fun Player.addOwner(@Named("player") owner: Player, @Select fakePlayer: FakePlayer) {
        if (fpm.get(owner.uniqueId)!= null) throw OwnerMustBeHumanException(owner.name, fakePlayer.name)
        if (fakePlayer.isOwnedBy(owner.uniqueId)) throw OwnerAlreadyBoundException(owner.name ,fakePlayer.name)
        launch {
            fpm.addOwner(fakePlayer,owner.uniqueId)
            sendLocalizedMessage("fakeplayer.owner.add.success", owner.name,fakePlayer.name)
        }
    }

    @Subcommand("owner remove")
    @Permission(OWNER_REMOVE)
    fun Player.removeOwner(@Named("player") owner: Player, @Select fakePlayer: FakePlayer) {
        if (owner.uniqueId == fakePlayer.creator?.uuid) throw OwnerIsCreatorCannotBeRemovedException(owner.name ,fakePlayer.name)
        if (!fakePlayer.isOwnedBy(owner.uniqueId)) throw OwnerNotBoundCannotBeRemovedException(owner.name ,fakePlayer.name)
        launch {
            fpm.removeOwner(fakePlayer,owner.uniqueId)
            sendLocalizedMessage("fakeplayer.owner.remove.success", owner.name,fakePlayer.name)
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
            sendLocalizedMessage("fakeplayer.database.import-data.success", result)
        }
    }

    @Subcommand("action")
    @Permission(ACTION)
    @HelpLine("fakeplayer.help.cmd.action", children = [
        HelpLine("fakeplayer.help.cmd.action-start", "fp action start <action> [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.action-execute", "fp action execute <action> [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.action-stopall", "fp action stopall [name]", playerOnly = true),
        HelpLine("fakeplayer.help.cmd.action-stop", "fp action stop <action> [name]", playerOnly = true),
    ])
    fun Player.actionListUI(@Select fakePlayer: FakePlayer) {
        FakePlayerActionListDialog(fakePlayer, this).show(this)
    }

    @Subcommand("action start")
    @Permission(ACTION)
    fun Player.actionUI(@Named("action") action: Action, @Select fakePlayer: FakePlayer) {
        assertPermission("${ACTION.value}.$name")
        FakePlayerActionExecuteDialog(fakePlayer, action, this).show(this)
    }

    @Subcommand("action execute")
    @Permission(ACTION)
    fun CommandSender.executeAction(@Named("action") action: Action, modeAndParams: ActionModeAndParameters, @Select fakePlayer: FakePlayer) {
        assertPermission("${ACTION.value}.$name")
        fakePlayer.actions.execute(action, modeAndParams.mode, modeAndParams.parameters)
    }

    @Subcommand("action stopall")
    @Permission(ACTION)
    fun stopAllAction(@Select fakePlayer: FakePlayer) {
        fakePlayer.actions.stopAll()
    }

    @Subcommand("action stop")
    @Permission(ACTION)
    fun stopAction(@Named("action") action: Action, @Select fakePlayer: FakePlayer) {
        fakePlayer.actions.stop(action)
    }

}
