package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerSpawnedEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.isFolia
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitTask

class FakePlayerTicker(private val fpm: FakePlayerManager): Listener {

    // Folia Logic
    @EventHandler
    fun startFoliaTicker(event: FakePlayerSpawnedEvent) {
        if (!isFolia) return
        val fakePlayer = event.fakePlayer
        fakePlayer.ticking = true
        fakePlayer.player.scheduler.runAtFixedRate(plugin, { task ->
            if (fakePlayer.ticking) {
                try {
                    fakePlayer.nms.doTick()
                    fakePlayer.actions.doTick()
                } catch (_: Exception) {
                }
            } else {
                task.cancel()
            }
        }, null, 1L, 1L)
    }

    @EventHandler
    fun stopFoliaTicker(event: FakePlayerQuitEvent) {
        if (!isFolia) return
        event.fakePlayer.ticking = false
    }

    //Paper Logic
    var ticker : BukkitTask? = null
    @EventHandler
    fun startPaperTicker(event: FakePlayerSpawnedEvent) {
        if (isFolia) return
        if (fpm.fakeplayersCount() <= 0) {
            ticker?.cancel()
            ticker = null
            return
        }
        if (ticker != null) return
        ticker = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            fpm.fakeplayers().forEach { fakePlayer ->
                if (fakePlayer.ticking) {
                    try {
                        fakePlayer.nms.doTick()
                        fakePlayer.actions.doTick()
                    } catch (_: Exception) {
                    }
                }
            }
        }, 0L, 1L)
    }

    @EventHandler
    fun stopPaperTicker(event: FakePlayerQuitedEvent) {
        if (isFolia) return
        if (fpm.fakeplayersCount() > 0) return
        ticker?.cancel()
        ticker = null
    }

}