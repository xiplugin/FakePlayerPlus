package com.coderxi.plugin.fakeplayer.utils

import com.coderxi.plugin.fakeplayer.FakePlayerPlusPlugin
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler.CommandContext
import com.coderxi.plugin.fakeplayer.command.permission.Permission
import com.coderxi.plugin.fakeplayer.utils.coroutine.EntityDispatcher
import com.coderxi.plugin.fakeplayer.utils.coroutine.LocationDispatcher
import com.coderxi.plugin.fakeplayer.utils.coroutine.ServerDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.PropertyKey
import revxrsal.commands.exception.context.ErrorContext
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

val plugin by lazy { JavaPlugin.getPlugin(FakePlayerPlusPlugin::class.java) }
val isFolia by lazy { runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isSuccess }

// Bukkit扩展
fun CommandSender.uniqueId(): UUID =
    (this as? Player)?.uniqueId ?: UUID(0L, 0L)
fun CommandSender.hostAddress(): String =
    (this as? Player)?.address?.address?.hostAddress ?: "127.0.0.1"
fun CommandSender.hasPermission(permission: Permission, or: Permission = Permission.ADMIN) =
    hasPermission(permission.value) || hasPermission(or.value)
fun Player.teleportAsync(location: Location, sound: Sound): CompletableFuture<Boolean> =
    teleportAsync(location).thenApply { success -> success.also { if (it) location.world.playSound(location, sound, 1f, 1f) } }
fun CommandSender.assertPermission(permission: String, or: Permission = Permission.ADMIN) {
    if (!hasPermission(permission) || !hasPermission(or.value)) throw FakePlayerCommandException.NoPermissionException()
}

// Coroutines扩展
internal val globalDispatcher = ServerDispatcher()
val Plugin.dispatcher: CoroutineDispatcher get() = globalDispatcher
val Server.dispatcher: CoroutineDispatcher get() = globalDispatcher
val Location.dispatcher: CoroutineDispatcher get() = if (isFolia) LocationDispatcher(this) else globalDispatcher
val Entity.dispatcher: CoroutineDispatcher get() = if (isFolia) EntityDispatcher(this) else globalDispatcher
val CommandSender.dispatcher: CoroutineDispatcher get() = if (isFolia) if (this is Player) EntityDispatcher(this) else globalDispatcher else globalDispatcher
val FakePlayer.dispatcher: CoroutineDispatcher get() = if (isFolia) EntityDispatcher(player) else globalDispatcher
internal val globalCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
fun launch(block: suspend CoroutineScope.() -> Unit) = Dispatchers.Default.launch(block)
fun launch(context: CommandContext, block: suspend CoroutineScope.() -> Unit) = Dispatchers.Default.launch(context,block)
fun CoroutineDispatcher.launch(block: suspend CoroutineScope.() -> Unit) = globalCoroutineScope.launch(this, block = block)
fun CoroutineDispatcher.launch(context: CommandContext, block: suspend CoroutineScope.() -> Unit) = globalCoroutineScope.launch(this) {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (e : Throwable) {
        plugin.lamp.handleException(e, ErrorContext.executingFunction(context))
    }
}

// Plugin hook
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

// Plugin message
fun tl(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translate(key, *args)
fun tlp(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translateWithPrefix(key, *args)
fun tls(@PropertyKey(resourceBundle = "messages.messages") key: String, vararg args: Any) = plugin.messages.translateStringWithArgs(key, *args)