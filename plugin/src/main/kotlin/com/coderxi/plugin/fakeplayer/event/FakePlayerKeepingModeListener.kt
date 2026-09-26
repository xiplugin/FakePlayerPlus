package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings.KeepingMode
import com.coderxi.plugin.fakeplayer.utils.bukkit.UUID_ZERO
import com.coderxi.plugin.fakeplayer.utils.coroutine.launch
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerLoadEvent

class FakePlayerKeepingModeListener: PluginComponent, Listener {

    @EventHandler
    fun joinOnServerStartup(event: ServerLoadEvent) {
        if (event.type != ServerLoadEvent.LoadType.STARTUP) return
        launch { restoreByNames(fpm.findNamesBySettingFromRepository("keepingMode", "\"${KeepingMode.ALWAYS.name}\"")) }
    }

    @EventHandler
    fun joinOnSpawnerJoin(event: PlayerJoinEvent) {
        launch { restoreByNames(fpm.findNamesByCreatorUuidAndSetting(event.player.uniqueId, "keepingMode", "\"${KeepingMode.FOLLOW_SPAWNER.name}\"")) }
    }

    private fun restoreByNames(names: Collection<String>) {
        if (names.isEmpty()) return
        plugin.logger.info("restoring ${names.size} fakeplayers : $names")
        names.forEach { name ->
            val location = Bukkit.getOfflinePlayer(name).location ?: return@forEach
            launch {
                val fakePlayer = fpm.spawn(name, Bukkit.getConsoleSender(), location) ?: return@launch
                val actionStates = fpm.getActiveActionStatesFromRepository(fakePlayer.uuid)
                if (actionStates.isEmpty()) return@launch
                actionStates.forEach { state ->
                    val action = plugin.globalActionRegistry.getType(state.action)?.getConstructor()?.newInstance() ?: return@launch
                    fakePlayer.actions.execute(action, state.mode, state.parameters)
                }
            }
        }
    }

    @EventHandler
    fun quitOnSpawnerQuited(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        if (fpm.isFake(uuid)) return
        val targets = fpm.fakeplayers().filter {
            (it.spawner.uuid == uuid || (it.spawner.uuid == UUID_ZERO && it.owners.any { p -> p.uuid == uuid }) ) &&
            (it.settings.keepingMode == KeepingMode.FOLLOW_SPAWNER || it.settings.keepingMode == KeepingMode.FOLLOW_SPAWNER_QUIT)
        }
        targets.forEach { fakePlayer ->
            fakePlayer.player.scheduler.runDelayed(plugin, { fakePlayer.quit("Follow Spawner Quiting (${event.player.name})") }, null , 5)
        }
    }

}