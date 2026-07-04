package com.coderxi.plugin.fakeplayer.utils.coroutine

import com.coderxi.plugin.fakeplayer.utils.plugin
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.*

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ServerDispatcher : CoroutineDispatcher(), Delay {

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !plugin.server.isGlobalTickThread
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (plugin.isEnabled) plugin.server.globalRegionScheduler.execute(plugin, block) else block.run()
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val ticks = ((timeMillis + 49) / 50).coerceAtLeast(1L)
        val task = plugin.server.globalRegionScheduler.runDelayed(plugin, { _ -> continuation.resume(Unit) }, ticks)
        continuation.invokeOnCancellation { task.cancel() }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val ticks = ((timeMillis + 49) / 50).coerceAtLeast(1L)
        val task = plugin.server.globalRegionScheduler.runDelayed(plugin, { _ -> block.run() }, ticks)
        return DisposableHandle { task.cancel() }
    }

}