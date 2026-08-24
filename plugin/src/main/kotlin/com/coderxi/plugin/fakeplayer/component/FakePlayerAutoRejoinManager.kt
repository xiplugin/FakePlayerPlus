package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.repository.FakePlayerRepository
import com.coderxi.plugin.fakeplayer.utils.launch
import com.coderxi.plugin.fakeplayer.utils.onPluginReload
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent

class FakePlayerAutoRejoinManager(
    private val fpm: FakePlayerManager,
    private val repository: FakePlayerRepository
) : Listener {

    init {
        onPluginReload(::onLoad)
    }

    @EventHandler
    fun onServerLoad(event: ServerLoadEvent) {
        if (event.type == ServerLoadEvent.LoadType.STARTUP) {
            onLoad()
        }
    }

    fun onLoad() {
        launch {
            // 延遲等待伺服器世界與區塊完全加載完成
            delay(1500L)
            val autoRejoinList = repository.findAutoRejoinFakePlayers()
            for (savedPlayer in autoRejoinList) {
                if (fpm.get(savedPlayer.name) != null) continue

                val offline = Bukkit.getOfflinePlayer(savedPlayer.uuid)
                val spawnLoc = offline.location ?: Bukkit.getWorlds().firstOrNull()?.spawnLocation
                if (spawnLoc != null) {
                    runCatching {
                        fpm.spawn(savedPlayer.name, Bukkit.getConsoleSender(), spawnLoc)
                    }
                }
            }

            delay(1000L)
            val flattenRepo = com.coderxi.plugin.fakeplayer.repository.FlattenRepository()
            val activeTasks = flattenRepo.findAllActiveTasks()
            for ((fakeUuid, task) in activeTasks) {
                val fakePlayer = fpm.get(fakeUuid) ?: continue
                if (fakePlayer.player.isOnline) {
                    val current = fakePlayer.actions.getActiveActions()[com.coderxi.plugin.fakeplayer.api.action.ActionType.FLATTEN.track]
                    if (current == null) {
                        fakePlayer.actions.dispatch(task)
                        fakePlayer.owners.forEach { owner ->
                            owner.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.resume", fakePlayer.name))
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onFakePlayerSpawned(event: com.coderxi.plugin.fakeplayer.api.event.FakePlayerSpawnedEvent) {
        val fakePlayer = event.fakePlayer
        val flattenRepo = com.coderxi.plugin.fakeplayer.repository.FlattenRepository()
        val task = flattenRepo.loadTask(fakePlayer.uuid) ?: return
        if (fakePlayer.player.isOnline) {
            val current = fakePlayer.actions.getActiveActions()[com.coderxi.plugin.fakeplayer.api.action.ActionType.FLATTEN.track]
            if (current == null) {
                fakePlayer.actions.dispatch(task)
                fakePlayer.owners.forEach { owner ->
                    owner.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.resume", fakePlayer.name))
                }
            }
        }
    }

}
