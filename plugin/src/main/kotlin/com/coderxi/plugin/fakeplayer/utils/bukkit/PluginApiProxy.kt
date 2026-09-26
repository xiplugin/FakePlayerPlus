package com.coderxi.plugin.fakeplayer.utils.bukkit

import org.bukkit.plugin.Plugin

abstract class PluginApiProxy(plugin: Plugin) {

    val classLoader: ClassLoader? = plugin.javaClass.classLoader
    fun getClass(className: String) = runCatching { classLoader?.loadClass(className) }.getOrNull()

}