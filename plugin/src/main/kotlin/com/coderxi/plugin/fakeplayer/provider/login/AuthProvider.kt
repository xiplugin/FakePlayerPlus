package com.coderxi.plugin.fakeplayer.provider.login

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.Bukkit.getPluginManager

interface AuthProvider {

    val enabled : Boolean

    fun forceAuth(fakePlayer: FakePlayer)

    abstract class ByPlugin(val hookedPluginName: String): AuthProvider {

        val hookedPlugin by lazy { getPluginManager().getPlugin(hookedPluginName) }

        override val enabled get() = hookedPlugin != null

    }

}