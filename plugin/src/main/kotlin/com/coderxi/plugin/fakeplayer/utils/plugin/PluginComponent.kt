package com.coderxi.plugin.fakeplayer.utils.plugin

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginComponent

interface PluginComponent: FakePlayerPlusPluginComponent {

    val plugin get() = com.coderxi.plugin.fakeplayer.plugin

}