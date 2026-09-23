package com.coderxi.plugin.fakeplayer.command.parameter

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.plugin
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

class ActionModeAndParametersParameterType: ParameterType<BukkitCommandActor, ActionModeAndParameters> {

    override fun parse(
        input: MutableStringStream,
        context: ExecutionContext<BukkitCommandActor>
    ): ActionModeAndParameters? {
        val expression = input.readString()
        if (expression.isEmpty()) return null
        val (mode, parameters) = expression.indexOf('[').let { index ->
            if (index != -1 && expression.endsWith("]")) {
                expression.substring(0, index) to parseParameters(expression.substring(index + 1, expression.length - 1))
            } else {
                expression to emptyMap()
            }
        }
        return ActionModeAndParameters(mode, parameters)
    }

    private fun parseParameters(paramsText: String): Map<String, String> {
        if (paramsText.isBlank()) return emptyMap()
        return paramsText.split(",")
            .map { it.split("=") }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }
    }

    override fun defaultSuggestions() = DefaultSuggestions

    object DefaultSuggestions : SuggestionProvider<BukkitCommandActor> {

        val registry get() = plugin.globalActionRegistry
        private val emptySuggestions = emptyList<String>()

        override fun getSuggestions(context: ExecutionContext<BukkitCommandActor?>): Collection<String?> {
            val action = context.getResolvedArgumentOrNull<Any>("action") ?: return emptySuggestions
            val actionModes = (if (action is Action) registry.getModes(action.javaClass) else registry.getModes(action.toString())) ?: return emptySuggestions
            return actionModes.map { mode ->
                val suggestParameters = registry.getModeSuggestParameters(mode)
                if (suggestParameters.isEmpty()) mode else "\"$mode[${suggestParameters.entries.joinToString{ it.key + "=" + it.value }}]\""
            }
        }
    }

}