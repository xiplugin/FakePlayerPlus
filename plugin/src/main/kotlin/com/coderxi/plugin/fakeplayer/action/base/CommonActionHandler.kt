package com.coderxi.plugin.fakeplayer.action.base

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class CommonActionHandler<T : Action>(
    val type: Class<T>,
    val name: String,
    val modes: Set<CommonActionMode>
) : ActionHandler<T> {

    private val startTicks = ConcurrentHashMap<UUID, Int>()

    final override fun onTick(
        fakePlayer: FakePlayer,
        action: T,
        mode: String,
        parameters: Map<String, String>
    ) {
        when (mode) {
            "once" -> {
                runOnce(fakePlayer, action)
                fakePlayer.actions.stop(action)
            }
            "interval" -> {
                val currentTick = Bukkit.getCurrentTick()
                val startTick = startTicks.getOrPut(fakePlayer.uuid) { currentTick }
                val intervalTicks = parameters["intervalTicks"]?.toIntOrNull()?.takeIf { it > 0 } ?: 20
                if ((currentTick - startTick) % intervalTicks != 0) return
                runIntervalTick(fakePlayer, action)
            }
            "continuous" -> {
                runContinuousTick(fakePlayer, action)
            }
        }
    }

    final override fun onStop(
        fakePlayer: FakePlayer,
        action: T,
        mode: String,
        parameters: Map<String, String>
    ) {
        startTicks.remove(fakePlayer.uuid)
        when (mode) {
            "once" -> stopOnce(fakePlayer, action)
            "interval" -> stopInterval(fakePlayer, action)
            "continuous" -> stopContinuous(fakePlayer, action)
        }
    }

    abstract fun runOnce(fakePlayer: FakePlayer, action: T)

    abstract fun runIntervalTick(fakePlayer: FakePlayer, action: T)

    abstract fun runContinuousTick(fakePlayer: FakePlayer, action: T)

    open fun stopOnce(fakePlayer: FakePlayer, action: T) {}

    open fun stopInterval(fakePlayer: FakePlayer, action: T) {}

    open fun stopContinuous(fakePlayer: FakePlayer, action: T) {}



}