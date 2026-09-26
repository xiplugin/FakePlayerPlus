package com.coderxi.plugin.fakeplayer.action

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionController
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.model.ActionState as ActionState0
import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.utils.coroutine.launch
import java.util.concurrent.ConcurrentHashMap

class ActionControllerImpl(private val fakePlayer: FakePlayer) : ActionController {

    val registry get() = plugin.globalActionRegistry

    private val activeActionStates = ConcurrentHashMap<Class<Action>, ActionState>()
    override val activeActions get() = activeActionStates.keys

    data class ActionState(
        val action: Action,
        val mode: String,
        val parameters: Map<String, String>,
    )

    override fun execute(action: Action, mode: String, parameters: Map<String, String>) {
        activeActionStates[action.javaClass] = ActionState(action, mode, parameters)
        if (!mode.equals("once", ignoreCase = true)) saveActionsAsync()
    }

    override fun doTick() {
        val iterator = activeActionStates.values.iterator()
        while (iterator.hasNext()) {
            val state = iterator.next()
            val action = state.action
            val handler = registry.getHandler(action.javaClass, state.mode)
            handler?.onTick(fakePlayer, action, state.mode, state.parameters)
        }
    }

    override fun stop(action: Action) {
        activeActionStates.remove(action.javaClass)?.let { state ->
            registry.getHandler(action.javaClass, state.mode)?.onStop(fakePlayer,action, state.mode, state.parameters)
            saveActionsAsync()
        }
    }

    override fun stopAll() {
        val iterator = activeActionStates.entries.iterator()
        while (iterator.hasNext()) {
            val (type, state) = iterator.next()
            iterator.remove()
            registry.getHandler(type, state.mode)?.onStop(fakePlayer, state.action, state.mode, state.parameters)
        }
        saveActionsAsync()
    }

    private fun saveActionsAsync() {
        launch {
            plugin.fakePlayerManager.saveActions(fakePlayer.uuid, activeActionStates.entries.map { (type, state) -> ActionState0(registry.getName(type)!!, state.mode, state.parameters) })
        }
    }

}