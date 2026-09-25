package com.coderxi.plugin.fakeplayer.api.action

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

interface ActionHandler<T : Action> {

    fun onTick(fakePlayer: FakePlayer, action: T, mode: String, parameters: Map<String, String>)

    fun onStop(fakePlayer: FakePlayer, action: T, mode: String, parameters: Map<String, String>) {}

}