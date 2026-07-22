package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.action.ActionMode.Once
import com.coderxi.plugin.fakeplayer.api.action.UseItemAction
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.dispatcher
import com.coderxi.plugin.fakeplayer.utils.launch
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

class FakePlayerAutoFishListener(private val fpm: FakePlayerManager) : Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.BITE) return
        val fakePlayer = fpm.get(event.player.uniqueId)?.takeIf { it.settings.autoFish } ?: return
        fakePlayer.dispatcher.launch {
            delay(50)
            fakePlayer.actions.dispatch(UseItemAction(Once))
            delay(1000)
            fakePlayer.actions.dispatch(UseItemAction(Once))
        }
    }

}