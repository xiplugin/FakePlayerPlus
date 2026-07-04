package com.coderxi.plugin.fakeplayer.utils.coroutine

import com.coderxi.plugin.fakeplayer.utils.plugin
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.resumeWithException

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class EntityDispatcher(private val entity: org.bukkit.entity.Entity) : CoroutineDispatcher(), Delay {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!entity.isValid) throw RejectedExecutionException("Entity is invalid")
        entity.scheduler.run(plugin, { _ -> block.run() }, { throw RejectedExecutionException("Task retired") })
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        if (!entity.isValid) {
            continuation.resumeWithException(RejectedExecutionException("Entity is invalid"))
            return
        }
        val ticks = (timeMillis / 50).coerceAtLeast(1L)
        val task = entity.scheduler.runDelayed(plugin, { _ -> continuation.resume(Unit) }, { if (continuation.isActive) continuation.resumeWithException(RejectedExecutionException("Task retired")) }, ticks)
        continuation.invokeOnCancellation { task?.cancel() }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        if (!entity.isValid) return DisposableHandle {}
        val ticks = ((timeMillis + 49) / 50).coerceAtLeast(1)
        val task = entity.scheduler.runDelayed(plugin, { _ -> block.run() }, null, ticks)
        return DisposableHandle { task?.cancel() }
    }

}