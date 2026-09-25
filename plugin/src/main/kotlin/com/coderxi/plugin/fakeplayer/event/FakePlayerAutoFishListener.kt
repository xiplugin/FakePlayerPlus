package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.UseItemAction
import com.coderxi.plugin.fakeplayer.utils.coroutine.dispatcher
import com.coderxi.plugin.fakeplayer.utils.coroutine.launch
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

class FakePlayerAutoFishListener : PluginComponent, Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.BITE) return
        val fakePlayer = fpm.get(event.player.uniqueId)?.takeIf { it.settings.autoFish } ?: return
        fakePlayer.dispatcher.launch {
            delay(50)
            fakePlayer.actions.execute(UseItemAction(), CommonActionMode.ONCE.key)
            delay(1000)
            fakePlayer.actions.execute(UseItemAction(), CommonActionMode.ONCE.key)
        }
    }

}