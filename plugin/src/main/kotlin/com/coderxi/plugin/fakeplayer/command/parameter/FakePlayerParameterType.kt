package com.coderxi.plugin.fakeplayer.command.parameter

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.NotExitsException
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.NotOwnerException
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.NoSelectedException
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ADMIN
import com.coderxi.plugin.fakeplayer.component.FakePlayerSelector.selected
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.plugin
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

class FakePlayerParameterType(private val fpm : FakePlayerManager) : ParameterType<BukkitCommandActor, FakePlayer> {

    override fun isGreedy() = true

    override fun parse(input: MutableStringStream, context: ExecutionContext<BukkitCommandActor>): FakePlayer? {
        val name = input.readString()
        val sender = context.actor().sender()
        if (sender is ConsoleCommandSender) {
            return fpm.get(name) ?: throw NotExitsException(name)
        }
        if (sender is Player) {
            if (name.isEmpty()) {
                return sender.selected ?: throw NoSelectedException()
            }
            val selected = fpm.get(name) ?: throw NotExitsException(name)
            if (!selected.ownerUuids.contains(sender.uniqueId) && !sender.hasPermission(ADMIN)) {
                throw NotOwnerException(selected.name)
            }
            return selected
        }
        return null
    }


    val defaultSuggestions = DefaultSuggestions()

    override fun defaultSuggestions() = defaultSuggestions

    class DefaultSuggestions : SuggestionProvider<BukkitCommandActor> {

        val fpm get() = plugin.fakePlayerManager
        private val emptySuggestions = listOf<String>()

        override fun getSuggestions(context: ExecutionContext<BukkitCommandActor?>): Collection<String?> {
            val sender = context.actor().sender()
            if (sender.hasPermission(ADMIN)) {
                return fpm.fakeplayers().map { it.name }
            } else if (sender is Player) {
                return fpm.fakeplayersByOwnerUuid(sender.uniqueId).map { it.name }
            }
            return emptySuggestions
        }
    }

}