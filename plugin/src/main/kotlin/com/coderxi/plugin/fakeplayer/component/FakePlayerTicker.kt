package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.PluginComponent

class FakePlayerTicker(private val fpm: FakePlayerManager) : PluginComponent {

    init {
        val ticker = scheduler.runTaskTimer(plugin, ::tick, 0L, 1L)
        onPluginDisable(ticker::cancel)
    }

    private fun tick() {
        if (fpm.fakeplayersCount() <= 0) return
        fpm.fakeplayers().forEach { fakePlayer ->
            try {
                fakePlayer.nms.doTick()
                fakePlayer.actions.doTick()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}