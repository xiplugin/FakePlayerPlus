package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerPreparingEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerSpawnedEvent
import com.coderxi.plugin.fakeplayer.utils.isFolia
import com.coderxi.plugin.fakeplayer.utils.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import kotlin.run

class FakePlayerLifecycleCommandListener: Listener {

    val commands get() = plugin.config.lifecycleCommands

    @EventHandler
    fun onFakePlayerPreparingEvent(event: FakePlayerPreparingEvent) = executeCommands(event.fakePlayer, commands.preparing, true)
    @EventHandler
    fun onFakePlayerConnectedEvent(event: FakePlayerConnectedEvent) = executeCommands(event.fakePlayer, commands.connected, false)
    @EventHandler
    fun onFakePlayerSpawnedEvent(event: FakePlayerSpawnedEvent) = executeCommands(event.fakePlayer, commands.spawned, false)
    @EventHandler
    fun onFakePlayerQuitEvent(event: FakePlayerQuitEvent) = executeCommands(event.fakePlayer, commands.quit, true)
    @EventHandler
    fun onFakePlayerQuitedEvent(event: FakePlayerQuitedEvent) = executeCommands(event.fakePlayer, commands.quited, true)

    private fun executeCommands(fakePlayer: FakePlayer, commands: List<String>, offlineExecute: Boolean) {
        val name = fakePlayer.name
        val uuid = fakePlayer.uuid.toString()
        val spawnerName = fakePlayer.spawnerName
        val spawnerUuid = fakePlayer.spawnerUuid
        val spawner = Bukkit.getPlayer(spawnerUuid)
        val console = Bukkit.getConsoleSender()
        commands.forEach {
            val command = it
                .replace("{uuid}", uuid)
                .replace("{name}", name)
                .replace("{spawner_uuid}", spawnerUuid.toString())
                .replace("{spawner_name}", spawnerName)
            when {
                it.startsWith("[CONSOLE]") -> listOf(console to command.removePrefix("[CONSOLE]").trimStart())
                it.startsWith("[SPAWNER]") -> listOf(spawner to command.removePrefix("[SPAWNER]").trimStart())
                it.startsWith("[OWNERS]") -> fakePlayer.owners.map { owner -> owner to command
                    .removePrefix("[OWNERS]").trimStart()
                    .replace("{owner_uuid}", owner.uniqueId.toString())
                    .replace("{owner_name}", owner.name)
                }
                else -> listOf(fakePlayer.player to command)
            }.forEach { (executor, command) ->
                if (executor == null || command.isBlank()) return@forEach
                if (isFolia) {
                    when (executor) {
                        is Player if offlineExecute -> {
                            plugin.server.regionScheduler.run(plugin, executor.location) { dispatchCommand(executor, command) }
                        }
                        is Player -> {
                            executor.scheduler.run(plugin, { dispatchCommand(executor, command) }, null)
                        }
                        else -> {
                            plugin.server.globalRegionScheduler.run(plugin) { dispatchCommand(executor, command) }
                        }
                    }
                } else {
                    dispatchCommand(executor, command)
                }
            }
        }
    }

    private fun dispatchCommand(executor: org.bukkit.command.CommandSender, command: String) {
        plugin.logger.info { "${executor.name} executing command: $command" }
        Bukkit.dispatchCommand(executor, command.removePrefix("/"))
    }

}