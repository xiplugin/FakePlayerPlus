package com.coderxi.plugin.fakeplayer.api

interface FakePlayerPlusPluginComponent {

    val priority get() = 0

    val fpm get() = FakePlayerPlusPluginApi.api.fakePlayerManager

    fun onReload() {}

    fun onDisable() {}

}