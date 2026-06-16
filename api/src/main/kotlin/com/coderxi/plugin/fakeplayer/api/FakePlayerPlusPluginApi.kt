package com.coderxi.plugin.fakeplayer.api

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

interface FakePlayerPlusPluginApi {

    val nms: NMSBridge

    val nmsServer: NMSServer

    val fakePlayerManager: FakePlayerManager

    companion object {
        val api by lazy { Bukkit.getPluginManager().getPlugin("FakePlayerPlus")!! as FakePlayerPlusPluginApi }
        val javaPlugin get() = api as JavaPlugin
    }

}