package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.provider.login.AuthMeAuthProvider
import com.coderxi.plugin.fakeplayer.provider.login.AuthProvider
import com.coderxi.plugin.fakeplayer.utils.coroutine.dispatcher
import com.coderxi.plugin.fakeplayer.utils.coroutine.launch
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class FakePlayerAutoAuthListener : PluginComponent, Listener {

    private val providers = listOf<AuthProvider>(
        AuthMeAuthProvider(),
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun autoAuth(event: FakePlayerConnectedEvent) {
        if (!plugin.config.msic.autoAuth) return
        val provider = providers.firstOrNull { it.enabled } ?: return
        event.fakePlayer.dispatcher.launch {
            provider.forceAuth(event.fakePlayer)
        }
    }

}