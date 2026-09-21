package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.event.FakePlayerDeathEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ADMIN
import com.coderxi.plugin.fakeplayer.config.DeathEventAction
import com.coderxi.plugin.fakeplayer.utils.dispatcher
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.launch
import com.coderxi.plugin.fakeplayer.utils.plugin
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class FakePlayerBehaviorImplementListener(private val fpm: FakePlayerManager): Listener {

    val config get() = plugin.config

    @EventHandler
    fun implementDeathAction(event: FakePlayerDeathEvent) {
        when (config.behavior.deathAction) {
            DeathEventAction.NONE -> { }
            DeathEventAction.QUIT -> {
                event.fakePlayer.player.scheduler.execute(plugin, { event.fakePlayer.quit() }, null, 1)
            }
            DeathEventAction.RESPAWN -> {
                event.fakePlayer.nms.respawn()
            }
            DeathEventAction.RESPAWN_BACK -> {
                event.fakePlayer.player.location.dispatcher.launch {
                    delay(1000)
                    event.fakePlayer.nms.respawn()
                    delay(50)
                    event.fakePlayer.player.lastDeathLocation?.let(event.fakePlayer.player::teleportAsync)
                }
            }
        }
    }

    @EventHandler
    fun implementFollowQuiting(event: PlayerQuitEvent) {
        if (!config.behavior.followQuiting) return
        val uuid = event.player.uniqueId
        val targets = if (event.player.hasPermission(ADMIN)) {
            fpm.fakeplayers().filter { it.spawner.uuid == uuid }
        } else {
            fpm.fakeplayersByOwnerUuid(uuid)
        }
        event.player.location.dispatcher.launch {
            delay(config.behavior.followQuitingDelay*1000L)
            targets.forEach { fakePlayer ->
                if (fakePlayer.owners.any { Bukkit.getPlayer(it.uuid) != null }) return@forEach
                fakePlayer.quit("Follow Quiting")
            }
        }
    }

}