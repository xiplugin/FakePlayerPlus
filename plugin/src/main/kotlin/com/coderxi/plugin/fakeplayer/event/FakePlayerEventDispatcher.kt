package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.event.*
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent

class FakePlayerEventDispatcher: PluginComponent, Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onFakePlayerQuit(event: PlayerQuitEvent) {
        fpm.get(event.player.uniqueId)?.let { FakePlayerQuitEvent(it,event.quitMessage()).callEvent() }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onFakePlayerPostQuit(event: PlayerQuitEvent) {
        fpm.get(event.player.uniqueId)?.let { fp -> plugin.server.globalRegionScheduler.runDelayed(plugin, {FakePlayerQuitedEvent(fp).callEvent()}, 1) }
    }

    @EventHandler
    fun onFakePlayerInteract(event: PlayerInteractEntityEvent) {
        if (event.rightClicked is Player) fpm.get(event.rightClicked.uniqueId)?.let { FakePlayerInteractedEvent(it,event.player,event.hand).callEvent() }
    }

}