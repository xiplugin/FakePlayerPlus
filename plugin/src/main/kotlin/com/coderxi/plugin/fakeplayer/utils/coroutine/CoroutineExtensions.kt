package com.coderxi.plugin.fakeplayer.utils.coroutine

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandExceptionHandler.CommandContext
import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.utils.bukkit.isFolia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import revxrsal.commands.exception.context.ErrorContext

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