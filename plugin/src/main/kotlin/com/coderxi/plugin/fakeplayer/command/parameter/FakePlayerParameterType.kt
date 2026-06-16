package com.coderxi.plugin.fakeplayer.command.parameter

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.NotExitsException
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

class FakePlayerParameterType : ParameterType<BukkitCommandActor, FakePlayer> {

    private val fpm get() = PluginComponent.plugin.fakePlayerManager

    override fun parse(input: MutableStringStream, context: ExecutionContext<BukkitCommandActor>): FakePlayer? {
        val name = input.readString()
        if (context.actor().isConsole) {
            return fpm.get(name) ?: throw NotExitsException(name)
        } else if (context.actor().isPlayer) {
            return fpm.get(name) ?: throw NotExitsException(name)
        }
        return null
    }

    override fun defaultSuggestions(): SuggestionProvider<BukkitCommandActor> {
        return SuggestionProvider { context ->
            if (context.actor().isConsole) {
                return@SuggestionProvider fpm.fakeplayers().map { it -> it.name }
            } else if (context.actor().isPlayer) {
                return@SuggestionProvider fpm.fakeplayersByOwnerUuid(context.actor().asPlayer()!!.uniqueId).map { it -> it.name }
            }
            return@SuggestionProvider listOf()
        }
    }
}