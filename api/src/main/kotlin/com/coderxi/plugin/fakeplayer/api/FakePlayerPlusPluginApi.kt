package com.coderxi.plugin.fakeplayer.api

import com.coderxi.plugin.fakeplayer.api.action.ActionRegistry
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginComponent as PluginComponent
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

interface FakePlayerPlusPluginApi {

    val nms: NMSBridge

    val nmsServer: NMSServer

    val fakePlayerManager: FakePlayerManager

    val globalActionRegistry: ActionRegistry

    /** 注册子组件(如果子组件为Listener会自动注册事件监听) */
    fun registerComponent(component: PluginComponent)

    companion object {
        val api by lazy { Bukkit.getPluginManager().getPlugin("FakePlayerPlus")!! as FakePlayerPlusPluginApi }
        val javaPlugin get() = api as JavaPlugin
    }

}