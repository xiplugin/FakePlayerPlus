package com.coderxi.plugin.fakeplayer.utils.coroutine

import com.coderxi.plugin.fakeplayer.utils.plugin
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import org.bukkit.Location

@OptIn(InternalCoroutinesApi::class,ExperimentalCoroutinesApi::class)
class LocationDispatcher(private val location: Location) : CoroutineDispatcher(), Delay {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        plugin.server.regionScheduler.execute(plugin, location, block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val ticks = (timeMillis / 50).coerceAtLeast(1L)
        val task = plugin.server.regionScheduler.runDelayed(plugin, location, { _ -> continuation.resume(Unit) }, ticks)
        continuation.invokeOnCancellation { task.cancel() }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val ticks = ((timeMillis + 49) / 50).coerceAtLeast(1)
        val task = plugin.server.regionScheduler.runDelayed(plugin, location, { _ -> block.run() }, ticks)
        return DisposableHandle { task.cancel() }
    }

}