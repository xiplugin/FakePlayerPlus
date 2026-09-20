package com.coderxi.plugin.fakeplayer.api.action

interface ActionRegistry {

    val actions: Map<Class<out Action>, String>

    fun <T : Action> register(
        type: Class<T>,
        name: String,
        mode: String,
        handler: ActionHandler<T>
    )

    fun getName(type: Class<out Action>): String?

    fun getType(name: String): Class<out Action>?

    fun getModes(type: Class<out Action>): List<String>?

    fun getModes(name: String): List<String>?

    fun setModeSuggestParameters(mode: String, suggestParameters: Map<String, Any>)

    fun getModeSuggestParameters(mode: String): Map<String, Any>

    fun <T : Action> getHandler(type: Class<T>, mode: String): ActionHandler<T>?

}