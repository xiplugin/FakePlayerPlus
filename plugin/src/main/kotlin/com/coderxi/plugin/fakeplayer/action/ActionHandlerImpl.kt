package com.coderxi.plugin.fakeplayer.action

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionTrack
import com.coderxi.plugin.fakeplayer.api.action.ActionMode.*
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.Bukkit
import java.util.EnumMap

class ActionHandlerImpl(private val fakePlayer: FakePlayer) : ActionHandler {

    private val activeActions = EnumMap<ActionTrack, ActionState>(ActionTrack::class.java)

    private class ActionState(
        val action: Action,
        var nextTick: Int = 0
    )

    override fun dispatch(action: Action) {
        stop(action.track)
        val state = ActionState(action, nextTick = Bukkit.getCurrentTick())
        activeActions[action.track] = state
    }

    override fun doTick() {
        val currentTick = Bukkit.getCurrentTick()
        val iterator = activeActions.values.iterator()
        while (iterator.hasNext()) {
            val state = iterator.next()
            if (currentTick >= state.nextTick) {
                ActionProcessorRegistry.get(state.action)?.process(fakePlayer, state.action, this)
                val action = state.action
                when (val actionMode = action.mode) {
                    is Once -> iterator.remove().let { stop(action) }
                    is Continuous -> state.nextTick = currentTick + 1
                    is Interval -> state.nextTick = currentTick + actionMode.intervalTicks
                }
            }
        }
    }

    override fun stop(track: ActionTrack) {
        activeActions.remove(track)?.action?.let {
            ActionProcessorRegistry.get(it)?.onStop(fakePlayer, it)
        }
    }

    override fun stop(action: Action) {
        ActionProcessorRegistry.get(action)?.onStop(fakePlayer, action)
        activeActions.remove(action.track)
    }

    override fun stopAll() {
        activeActions.keys.forEach(::stop)
    }

    override fun getActiveActions(): Map<ActionTrack, Action> {
        return activeActions.mapValues { it.value.action }
    }
}