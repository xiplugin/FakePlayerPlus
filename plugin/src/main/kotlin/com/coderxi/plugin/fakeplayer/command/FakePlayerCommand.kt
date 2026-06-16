package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.annotaion.SelectFlag
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Dependency
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Subcommand

@Command("fakeplayer","fp")
class FakePlayerCommand: PluginComponent {

    @Dependency
    lateinit var fpm: FakePlayerManager

    @Subcommand("reload")
    fun CommandSender.reload() {
        plugin.onReload()
        sendMessage(tlp("fakeplayer.reload.success"))
    }

    @Subcommand("spawn")
    fun Player.spawn(@Named("name") name: String) {
        assertNotExits(name)
        assertNoLimited()
        fpm.spawnAsync(name, uniqueId, location).thenApply { fakePlayer ->
            val locationText = "%.2f, %.2f, %.2f".format(location.x, location.y, location.z)
            sendMessage(tlp("fakeplayer.spawn.success", name, fakePlayer.world.name, locationText))
        }
    }

    @Subcommand("select")
    fun Player.select(fakePlayer: FakePlayer) {
        assertOwned(fakePlayer)
        //TODO
    }

    @Subcommand("remove")
    fun Player.remove(@SelectFlag fakePlayer: FakePlayer) {
        assertOwned(fakePlayer)
        fakePlayer.let { fpm.remove(name, this) }
        sendMessage(tlp("fakeplayer.remove.success", fakePlayer.name))
    }

    @Subcommand("invsee")
    fun Player.invsee(@SelectFlag fakePlayer: FakePlayer) {
        assertOwned(fakePlayer)
        InvseeProvider.current.openInventory(this,fakePlayer.player)
        playSound(location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)
    }

    @Subcommand("tp")
    fun Player.tp(@SelectFlag fakePlayer: FakePlayer) {
        assertOwned(fakePlayer)
        teleportAsync(fakePlayer.location)
    }

    @Subcommand("tphere")
    fun Player.tphere(@SelectFlag fakePlayer: FakePlayer) {
        assertOwned(fakePlayer)
        fakePlayer.player.teleportAsync(location)
    }

    @Subcommand("tpswap")
    fun Player.tpswap(@SelectFlag fakePlayer: FakePlayer) {
        assertOwned(fakePlayer)
        val playerLocation = location
        teleportAsync(fakePlayer.location)
        fakePlayer.player.teleportAsync(playerLocation)
    }

    @Subcommand("tppos")
    fun Player.tppos(location: Location, @SelectFlag fakePlayer: FakePlayer) {
        assertOwned(fakePlayer)
        fakePlayer.player.teleportAsync(location)

    }


    fun Player.assertOwned(fakePlayer: FakePlayer) {
        if (!fpm.isOwned(this,fakePlayer)) throw NotOwnerException(fakePlayer.name)
    }
    fun Player.assertNotExits(name: String) {
        if (fpm.get(name)!=null) throw SpawnAlreadyExistsException(name)
    }
    fun Player.assertNoLimited() {
        if (fpm.fakeplayersCount()>=plugin.config.spawnLimit.server) throw SpawnServerLimitedException()
        if (fpm.fakeplayersByOwnerUuid(uniqueId).count() > 3) throw SpawnPlayerLimitedException()
    }

}