package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import org.bukkit.scheduler.BukkitTask

class FakePlayerTicker(private val fpm: FakePlayerManager) : PluginComponent {

    init { onPluginDisable { stop() } }

    private var masterTask: BukkitTask? = null

    fun start() {
        if (masterTask != null) return
        masterTask = scheduler.runTaskTimer(plugin, this::run, 0L, 1L)
    }

    private fun run() {
        if (fpm.fakeplayersCount() <= 0) return
        fpm.fakeplayers().forEach { fakePlayer ->
            try {
                fakePlayer.doTick()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        masterTask?.cancel()
        masterTask = null
    }

}