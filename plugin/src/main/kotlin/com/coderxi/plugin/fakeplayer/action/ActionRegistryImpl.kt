package com.coderxi.plugin.fakeplayer.action

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionRegistry
import java.util.concurrent.ConcurrentHashMap

class ActionRegistryImpl : ActionRegistry {

    private val type2name = ConcurrentHashMap<Class<out Action>, String>()
    private val name2type = ConcurrentHashMap<String, Class<out Action>>()

    override val actions get() = type2name
    private val type2modes = ConcurrentHashMap<Class<out Action>, MutableList<String>>()

    private val mode2params = ConcurrentHashMap<String, Map<String, Any>>()

    private data class Key(val type: Class<out Action>, val mode: String)
    private val handlers = ConcurrentHashMap<Key, ActionHandler<*>>()


    override fun <T : Action> register(
        type: Class<T>,
        name: String,
        mode: String,
        handler: ActionHandler<T>
    ) {
        unsafeRegister(type, name, mode, handler)
    }

    fun registerCommonHandlers(vararg handlers: CommonActionHandler<*>) {
        handlers.forEach { handler ->
            handler.modes.forEach { mode ->
                unsafeRegister(
                    handler.type,
                    handler.name,
                    mode.key,
                    handler
                )
            }
        }
    }

    internal fun unsafeRegister(
        type: Class<out Action>,
        name: String,
        mode: String,
        handler: ActionHandler<out Action>
    ) {
        type2name[type] = name
        name2type[name] = type
        type2modes.computeIfAbsent(type) { mutableListOf() }.add(mode)
        handlers[Key(type, mode)] = handler
    }

    override fun getName(type: Class<out Action>): String? {
        return type2name[type]
    }

    override fun getType(name: String): Class<out Action>? {
        return name2type[name]
    }

    override fun getModes(type: Class<out Action>): List<String>? {
        return type2modes[type]
    }

    override fun getModes(name: String): List<String>? {
        val type = name2type[name] ?: return null
        return getModes(type)
    }

    override fun setModeSuggestParameters(mode: String, suggestParameters: Map<String, Any>) {
        if (suggestParameters.isNotEmpty()) mode2params[mode] = suggestParameters
    }

    override fun getModeSuggestParameters(mode: String): Map<String, Any> {
        return mode2params[mode] ?: emptyMap()
    }

    override fun <T : Action> getHandler(type: Class<T>, mode: String): ActionHandler<T>? {
        @Suppress("UNCHECKED_CAST")
        return handlers[Key(type, mode)] as? ActionHandler<T>
    }


}