package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerPreparingEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerSpawnedEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class FakePlayerLifecycleCommandListener (private val fpm: FakePlayerManager): Listener, PluginComponent {

    val commands get() = plugin.config.lifecycleCommands

    private fun executeCommands(fakePlayer: FakePlayer, commands: List<String>) {
        val fakePlayerSpawner = fakePlayer.spawner
        commands.forEach { express ->
            if (express.startsWith("[CONSOLE]", ignoreCase = true)) {
                val command = commandWithVars(express.removePrefix("[CONSOLE]").trimStart(), fakePlayer, fakePlayerSpawner)
                dispatchCommand(Bukkit.getConsoleSender(),  command)
            } else if (express.startsWith("[SPAWNER]", ignoreCase = true)) {
                val command = commandWithVars(express.removePrefix("[SPAWNER]").trimStart(), fakePlayer, fakePlayerSpawner)
                dispatchCommand(fakePlayerSpawner, command)
            } else if (express.startsWith("[OWNERS]", ignoreCase = true)) {
                fakePlayer.owners.forEach { fakePlayerOwner ->
                    val command = commandWithVars(express.removePrefix("[OWNERS]").trimStart(), fakePlayer, fakePlayerSpawner, fakePlayerOwner)
                    dispatchCommand(fakePlayerOwner, command)
                }
            } else {
                dispatchCommand(fakePlayer.player, commandWithVars(express, fakePlayer, fakePlayerSpawner))
            }
        }
    }

    private fun commandWithVars(command: String, fakePlayer: FakePlayer, spawner: Player? = null, owner: Player? = null): String {
        var cmd = command
            .replace("{name}", fakePlayer.name)
            .replace("{uuid}", fakePlayer.uuid.toString())
        if (spawner != null) cmd = cmd
            .replace("{spawner_name}", spawner.name)
            .replace("{spawner_uuid}", spawner.uniqueId.toString())
        if (owner != null) cmd = cmd
            .replace("{owner_name}", owner.name)
            .replace("{owner_uuid}", owner.uniqueId.toString())
        return cmd
    }

    private fun dispatchCommand(commandExecutor: CommandSender, command: String): Boolean {
        plugin.logger.info { "${commandExecutor.name} executing command: $command" }
        return Bukkit.dispatchCommand(commandExecutor, command.removePrefix("/"))
    }

    @EventHandler fun onFakePlayerPreparingEvent(event: FakePlayerPreparingEvent) = executeCommands(event.fakePlayer, commands.preparing)
    @EventHandler fun onFakePlayerConnectedEvent(event: FakePlayerConnectedEvent) = executeCommands(event.fakePlayer, commands.connected)
    @EventHandler fun onFakePlayerSpawnedEvent(event: FakePlayerSpawnedEvent) = executeCommands(event.fakePlayer, commands.spawned)
    @EventHandler fun onFakePlayerQuitEvent(event: FakePlayerQuitEvent) = executeCommands(event.fakePlayer, commands.quit)
    @EventHandler fun onFakePlayerQuitedEvent(event: FakePlayerQuitedEvent) = executeCommands(event.fakePlayer, commands.quited)

}