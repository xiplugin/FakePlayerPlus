package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class FakePlayerDummyVarsNotifyListener: PluginComponent, Listener {

    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        if (fpm.get(event.player.uniqueId) != null) return
        fpm.fakeplayers().forEach { it.nms.dummyNotify(listOf(event.player)) }
    }

}