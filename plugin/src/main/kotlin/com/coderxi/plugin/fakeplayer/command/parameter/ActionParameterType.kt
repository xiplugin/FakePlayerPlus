package com.coderxi.plugin.fakeplayer.command.parameter

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.NoPermissionException
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.plugin
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission as Permission

class ActionParameterType: ParameterType<BukkitCommandActor, Action> {

    override fun parse(
        input: MutableStringStream,
        context: ExecutionContext<BukkitCommandActor>
    ): Action? {
        val actionName = input.readString()
        val sender = context.actor().sender()
        val actionType = plugin.globalActionRegistry.getType(actionName) ?: return null
        val permissions = actionType.getAnnotation(Permission::class.java)
        if (permissions != null && !sender.hasPermission(permissions.node, permissions.or)) {
            throw NoPermissionException()
        }
        return actionType.getConstructor().newInstance() as Action
    }

    override fun defaultSuggestions() = DefaultSuggestions

    object DefaultSuggestions: SuggestionProvider<BukkitCommandActor> {
        override fun getSuggestions(context: ExecutionContext<BukkitCommandActor?>): Collection<String?> {
            return plugin.globalActionRegistry.actions.mapNotNull { actionType ->
                val sender = context.actor().sender()
                val permissions = actionType.getAnnotation(Permission::class.java)
                if (permissions != null && !sender.hasPermission(permissions.node, permissions.or)) {
                    null
                } else {
                    plugin.globalActionRegistry.getName(actionType)
                }
            }
        }
    }
}