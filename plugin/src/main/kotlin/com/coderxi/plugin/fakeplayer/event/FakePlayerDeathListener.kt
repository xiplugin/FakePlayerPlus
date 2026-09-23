package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings.DeathAction.*
import com.coderxi.plugin.fakeplayer.utils.coroutine.dispatcher
import com.coderxi.plugin.fakeplayer.utils.coroutine.launch
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class FakePlayerDeathListener: PluginComponent, Listener {

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val fakePlayer = fpm.get(event.player.uniqueId) ?: return

        event.keepLevel = true
        event.droppedExp = 0
        event.deathMessage(null)

        if (fakePlayer.settings.keepInventory) {
            event.keepInventory = true;
            event.drops.clear()
        }

        when (fakePlayer.settings.deathAction) {
            NONE -> { }
            QUIT -> {
                fakePlayer.player.scheduler.execute(plugin, { fakePlayer.quit() }, null, 1)
            }
            RESPAWN -> {
                fakePlayer.nms.respawn()
            }
            RESPAWN_BACK -> {
                fakePlayer.player.location.dispatcher.launch {
                    delay(1000)
                    fakePlayer.nms.respawn()
                    delay(50)
                    fakePlayer.player.lastDeathLocation?.let(fakePlayer.player::teleportAsync)
                }
            }
        }
    }


}