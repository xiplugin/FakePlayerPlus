package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.event.*
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.*

class FakePlayerEventDispatcher(private val fpm: FakePlayerManager): Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onFakePlayerQuit(event: PlayerQuitEvent) {
        fpm.get(event.player.uniqueId)?.let { FakePlayerQuitEvent(it,event.quitMessage()).callEvent() }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onFakePlayerPostQuit(event: PlayerQuitEvent) {
        fpm.get(event.player.uniqueId)?.let { fp -> plugin.server.globalRegionScheduler.runDelayed(plugin, {FakePlayerQuitedEvent(fp).callEvent()}, 1) }
    }
    @EventHandler
    fun onFakePlayerDeath(event: PlayerDeathEvent) {
        fpm.get(event.player.uniqueId)?.let {
            if (plugin.config.behavior.keepInventory) {
                event.keepInventory = true;
                event.drops.clear()
            }
            event.keepLevel = true
            event.droppedExp = 0
            event.deathMessage(null);
            FakePlayerDeathEvent(it,event.player.location).callEvent() }
    }
    @EventHandler
    fun onFakePlayerRespawn(event: PlayerRespawnEvent) {
        fpm.get(event.player.uniqueId)?.let { FakePlayerRespawnEvent(it).callEvent() }
    }
    @EventHandler
    fun onFakePlayerInteract(event: PlayerInteractEntityEvent) {
        if (event.rightClicked is Player) fpm.get(event.rightClicked.uniqueId)?.let { FakePlayerInteractedEvent(it,event.player,event.hand).callEvent() }
    }

}