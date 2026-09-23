package com.coderxi.plugin.fakeplayer.utils.plugin

interface PluginComponent {

    val priority get() = 0

    val plugin get() = com.coderxi.plugin.fakeplayer.plugin

    val fpm get() = plugin.fakePlayerManager

    fun onReload() {}

    fun onDisable() {}

}