package com.coderxi.plugin.fakeplayer.context

import com.coderxi.plugin.fakeplayer.FakePlayerPlusPlugin
import com.coderxi.plugin.fakeplayer.utils.EventBus
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.PropertyKey

interface PluginContext {

    val plugin get() = Vars.plugin
    val bridge get() = plugin.bridge
    val nmsServer get() = Vars.nmsServer
    val config get() = plugin.config
    val logger get() = plugin.logger
    val namespace get() = Vars.namespace
    val scheduler get() = plugin.server.scheduler
    fun schedulerRun(action: () -> Unit) = scheduler.runTask(plugin, action)
    fun schedulerRunLaterAsync(delay: Long = 1, action: () -> Unit) = scheduler.runTaskLaterAsynchronously(plugin, action, delay)
    fun tl(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translate(key, *args)
    fun onPluginEnable(priority: Int = 0, action: (Any) -> Unit) = eventBus.registerEvent("Enable",priority,action)
    fun onPluginReload(priority: Int = 0, action: (Any) -> Unit) = eventBus.registerEvent("Reload",priority,action)
    fun onPluginDisable(priority: Int = 0, action: (Any) -> Unit) = eventBus.registerEvent("Disable",priority,action)

    private object Vars {
        val plugin by lazy { JavaPlugin.getPlugin(FakePlayerPlusPlugin::class.java) }
        val nmsServer by lazy { plugin.bridge.fromServer(plugin.server) }
        val namespace by lazy { NamespacedKey(plugin, "fakeplayer") }
    }

    companion object {
        private val eventBus = EventBus()
        fun emit(eventName: String) = eventBus.emit(eventName)
    }

}