package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.component.FakePlayerSelector.selected
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission as Permission
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler.CommandContext
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*
import com.coderxi.plugin.fakeplayer.component.FakePlayerLimiter
import com.coderxi.plugin.fakeplayer.component.FakePlayerDialog
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.utils.BukkitMain
import com.coderxi.plugin.fakeplayer.utils.SkinFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Cooldown
import revxrsal.commands.annotation.Dependency
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Subcommand
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@Command("fakeplayer","fp")
class FakePlayerCommand: PluginComponent {

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
    fun Player.spawn(commandContext: CommandContext) {
        val player = this
        assertNoSpawnLimited()
        launch(commandContext) {
            val name = fpm.sequenceName(player, ceil((fpl.getPlayerSpawnLimit(player)/10.0)).toInt())
            executeSpawn(name)
        }
    }


    @Subcommand("spawn")
    @Permission(SPAWN_WITH_NAME, ADMIN)
    fun CommandSender.spawn(@Named("name") name: String, commandContext: CommandContext) {
        val player = this as? Player
        if (!plugin.config.name.pattern.matches(name)) throw SpawnNameInvalidException(name)
        assertNoSpawnLimited()
        launch(commandContext) {
            if (fpm.get(name) != null) throw SpawnAlreadyExistsException(name)
            if (player != null && fpm.isNameUsed(name)) {
                val fakePlayer = fpm.getFromRepository(name)
                if (fakePlayer != null && fakePlayer.ownerUuids.isNotEmpty() && !fakePlayer.ownerUuids.contains(player.uniqueId)) {
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
    }

    @Subcommand("invsee")
    @Permission(INVSEE,BASIC)
    fun Player.invsee(@Select fakePlayer: FakePlayer) {
        InvseeProvider.current.openInventory(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("tp")
    @Permission(TP,BASIC)
    fun Player.tp(@Select fakePlayer: FakePlayer) {
        teleportAsync(fakePlayer.player.location)
    }

    @Subcommand("tphere")
    @Permission(TP,BASIC)
    fun Player.tphere(@Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location)
    }

    @Subcommand("tpswap")
    @Permission(TP,BASIC)
    fun Player.tpswap(@Select fakePlayer: FakePlayer) {
        val playerLocation = location
        teleportAsync(fakePlayer.player.location)
        fakePlayer.player.teleportAsync(playerLocation)
    }

    @Subcommand("tppos")
    @Permission(TP,BASIC)
    fun Player.tppos(location: Location, @Select fakePlayer: FakePlayer) {
        fakePlayer.player.teleportAsync(location)
    }

    @Subcommand("skin")
    @Permission(SKIN,BASIC)
    @Cooldown(value = 1, unit = TimeUnit.MINUTES)
    fun Player.skin(@Named("name") targetName: String, @Select fakePlayer: FakePlayer) {
        launch {
            val skin = SkinFetcher.getPlayerSkinInfoByName(targetName)
            withContext(Dispatchers.BukkitMain) {
                fakePlayer.skin = skin
                fakePlayer.player.world.playSound(fakePlayer.player.location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
            }
            fpm.saveSkin(fakePlayer)
        }
    }

    @Subcommand("cmd")
    @Permission(CMD,BASIC)
    fun Player.cmd(@Named("command") command: String, @Select fakePlayer: FakePlayer) {
        Bukkit.dispatchCommand(fakePlayer.player, command.removePrefix("/"))
    }

    @Subcommand("chat")
    @Permission(CHAT,BASIC)
    fun Player.message(@Named("message") message: String, @Select fakePlayer: FakePlayer) {
        fakePlayer.player.chat(message)
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

}