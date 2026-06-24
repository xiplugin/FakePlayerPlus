package com.coderxi.plugin.fakeplayer.utils

import com.coderxi.plugin.fakeplayer.FakePlayerPlusPlugin
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler.CommandContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.PropertyKey
import revxrsal.commands.exception.context.ErrorContext
import java.util.concurrent.CopyOnWriteArrayList

interface PluginComponent {

    val plugin get() = Companion.plugin
    val scheduler get() = plugin.server.scheduler
    fun tl(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translate(key, *args)
    fun tlp(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translateWithPrefix(key, *args)
    fun tls(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translateStringWithArgs(key, *args)
    fun onPluginReload(action: () -> Unit) = onPluginReload(0,action)
    fun onPluginReload(priority: Int, action: () -> Unit) = synchronized(reloadHandlers) {
        reloadHandlers.add(Hook(action, priority))
        reloadHandlers.sortWith(compareByDescending { it.priority })
    }
    fun onPluginDisable(action: () -> Unit) = onPluginDisable(0,action)
    fun onPluginDisable(priority: Int, action: () -> Unit) = synchronized(disableHandlers) {
        disableHandlers.add(Hook(action, priority))
        disableHandlers.sortWith(compareByDescending { it.priority })
    }
    fun launch(commandContext: CommandContext? = null, action: suspend CoroutineScope.() -> Unit) = defaultCoroutineScope.launch {
        try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e : Throwable) {
            if (commandContext == null) throw e
            plugin.lamp.handleException(e, ErrorContext.executingFunction(commandContext))
        }
    }
    companion object {
        val plugin by lazy { JavaPlugin.getPlugin(FakePlayerPlusPlugin::class.java) }
        private data class Hook(val action: () -> Unit, val priority: Int)
        private val reloadHandlers = CopyOnWriteArrayList<Hook>()
        private val disableHandlers = CopyOnWriteArrayList<Hook>()
        internal fun executeReload() = synchronized(reloadHandlers) {
            for (i in 0 until reloadHandlers.size) reloadHandlers[i].action()
        }
        internal fun executeDisable() = synchronized(disableHandlers) {
            for (i in 0 until disableHandlers.size) disableHandlers[i].action()
            reloadHandlers.clear()
            disableHandlers.clear()

        }
        private val defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
        internal val bukkitMainDispatcher = BukkitMainDispatcher(plugin)
    }

}