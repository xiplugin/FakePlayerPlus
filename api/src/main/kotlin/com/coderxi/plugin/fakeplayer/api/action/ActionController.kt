package com.coderxi.plugin.fakeplayer.api.action

interface ActionController {

    val activeActions: Collection<Class<Action>>

    fun execute(action: Action, mode: String, parameters: Map<String, String> = emptyMap())

    fun doTick()

    fun stop(action: Action)

    fun stopAll()

}