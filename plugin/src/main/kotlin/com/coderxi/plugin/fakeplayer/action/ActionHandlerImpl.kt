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
        synchronized(activeActions) {
            activeActions[action.track] = state
        }
    }

    override fun doTick() {
        val currentTick = Bukkit.getCurrentTick()
        val states = synchronized(activeActions) { activeActions.values.toList() }
        for (state in states) {
            if (currentTick >= state.nextTick) {
                ActionProcessorRegistry.get(state.action)?.process(fakePlayer, state.action, this)
                val action = state.action
                when (val actionMode = action.mode) {
                    is Once -> stop(action)
                    is Continuous -> state.nextTick = currentTick + 1
                    is Interval -> state.nextTick = currentTick + actionMode.intervalTicks
                }
            }
        }
    }

    override fun stop(track: ActionTrack) {
        val removed = synchronized(activeActions) {
            activeActions.remove(track)
        }
        removed?.action?.let {
            ActionProcessorRegistry.get(it)?.onStop(fakePlayer, it)
        }
    }

    override fun stop(action: Action) {
        val removed = synchronized(activeActions) {
            if (activeActions[action.track]?.action == action) {
                activeActions.remove(action.track)
            } else null
        }
        removed?.action?.let {
            ActionProcessorRegistry.get(it)?.onStop(fakePlayer, it)
        }
    }

    override fun stopAll() {
        val tracks = synchronized(activeActions) { activeActions.keys.toList() }
        tracks.forEach(::stop)
    }

    override fun getActiveActions(): Map<ActionTrack, Action> {
        return synchronized(activeActions) {
            activeActions.mapValues { it.value.action }
        }
    }
}