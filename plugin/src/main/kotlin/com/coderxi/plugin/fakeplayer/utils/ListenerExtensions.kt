package com.coderxi.plugin.fakeplayer.utils

import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

object ListenerExtensions {

    fun Listener.registerMyEvents(plugin: JavaPlugin = PluginComponent.plugin) {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

}