package com.coderxi.plugin.fakeplayer.command.parameter

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionMode
import com.coderxi.plugin.fakeplayer.api.action.ActionMode.*
import com.coderxi.plugin.fakeplayer.api.action.ActionType
import com.coderxi.plugin.fakeplayer.api.utils.ParamName
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.UnsupportedActionModeException
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

class ActionModeParameterType : ParameterType<BukkitCommandActor, ActionMode> {

    override fun parse(input: MutableStringStream, context: ExecutionContext<BukkitCommandActor>): ActionMode? {
        val expression = input.readString()
        if (expression.isEmpty()) return null
        val type = context.getResolvedArgumentOrNull(ActionType::class.java) ?: return null
        val (modeName,modeParams) = expression.indexOf('[').let { index ->
            if (index != -1 && expression.endsWith("]")) {
                expression.substring(0, index) to parseParamsToMap(expression.substring(index + 1, expression.length - 1))
            } else {
                expression to null
            }
        }
        val modeClass = Action.getSupportModes(type).find { it.simpleName.lowercase() == modeName } ?: throw UnsupportedActionModeException(expression)
        val modeConstructor = modeClass.declaredConstructors.first()
        return try {
            val instanceField = modeClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.get(null) as ActionMode
        } catch (e: NoSuchFieldException) {
            modeConstructor.newInstance(*modeConstructor.parameters.map { param ->
                val name = param.getAnnotation(ParamName::class.java).value
                when (param.type) {
                    Int::class.java, Int::class.javaPrimitiveType -> modeParams?.get(name)?.toInt()
                    Double::class.java, Double::class.javaPrimitiveType -> modeParams?.get(name)?.toDouble()
                    Float::class.java, Float::class.javaPrimitiveType -> modeParams?.get(name)?.toFloat()
                    Boolean::class.java, Boolean::class.javaPrimitiveType -> modeParams?.get(name)?.toBoolean()
                    String::class.java -> modeParams?.get(name)?:""
                    else -> null
                }
            }.toTypedArray<Any?>()) as ActionMode
        }
    }

    private val emptySuggestions = listOf<String>()

    override fun defaultSuggestions(): SuggestionProvider<BukkitCommandActor> = SuggestionProvider { context ->
        val type = context.getResolvedArgumentOrNull(ActionType::class.java) ?: return@SuggestionProvider emptySuggestions
        return@SuggestionProvider toSuggestions(Action.getSupportModes(type))
    }

    private fun toSuggestions(actionModes: List<Class<out ActionMode>>): Collection<String> {
        val suggestions = mutableListOf<String>()
        for (modeClass in actionModes) {
            when {
                Once::class.java.isAssignableFrom(modeClass) -> suggestions.add("\"once\"")
                Continuous::class.java.isAssignableFrom(modeClass) -> suggestions.add("\"continuous\"")
                Interval::class.java.isAssignableFrom(modeClass) -> suggestions.add("\"interval[intervalTicks=20]\"")
            }
        }
        return suggestions
    }

    private fun parseParamsToMap(paramsText: String): Map<String, String> {
        if (paramsText.isBlank()) return emptyMap()
        return paramsText.split(",")
            .map { it.split("=") }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }
    }
}