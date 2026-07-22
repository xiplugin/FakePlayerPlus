package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

sealed interface ActionProcessor<in T : Action> {

    val actionType: Class<out Action>

    fun process(fakePlayer: FakePlayer, action: T, handler: ActionHandler)

    fun onStop(fakePlayer: FakePlayer, action: T) {}

}