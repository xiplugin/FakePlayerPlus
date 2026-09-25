package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ADMIN
import com.coderxi.plugin.fakeplayer.command.permission.hasPermission
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class FakePlayerFollowQuittingListener: PluginComponent, Listener {

    @EventHandler
    fun followQuiting(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        if (fpm.isFake(uuid)) return
        val targets = if (event.player.hasPermission(ADMIN)) {
            fpm.fakeplayers().filter { it.spawner.uuid == uuid }
        } else {
            fpm.fakeplayersByOwnerUuid(uuid)
        }.filter {
            it.settings.followQuiting
        }
        targets.forEach { fakePlayer ->
            val delayTicks = fakePlayer.settings.followQuitingDelay.coerceAtLeast(1) * 20L
            fakePlayer.player.scheduler.runDelayed(plugin, { fakePlayer.quitIfNoOwnerOnline() }, null, delayTicks)
        }
    }

    private fun FakePlayer.quitIfNoOwnerOnline() {
        if (owners.any { Bukkit.getPlayer(it.uuid) != null }) return
        quit("Follow Quiting")
    }

}