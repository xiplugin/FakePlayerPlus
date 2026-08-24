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
        }
    }

}
