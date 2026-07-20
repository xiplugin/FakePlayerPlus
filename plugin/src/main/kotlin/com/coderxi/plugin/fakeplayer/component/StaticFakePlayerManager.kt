package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.config.StaticFakePlayersConfig
import com.coderxi.plugin.fakeplayer.config.StaticFakePlayersConfig.StaticFakePlayerMeta as Meta
import com.coderxi.plugin.fakeplayer.utils.SkinFetcher
import com.coderxi.plugin.fakeplayer.utils.dispatcher
import com.coderxi.plugin.fakeplayer.utils.launch
import com.coderxi.plugin.fakeplayer.utils.onPluginReload
import com.coderxi.plugin.fakeplayer.utils.plugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import java.util.concurrent.ConcurrentHashMap

class StaticFakePlayerManager(private val fpm: FakePlayerManager, private val config: StaticFakePlayersConfig) : Listener {

    private val spawnedNames = ConcurrentHashMap.newKeySet<String>()

    init {
        onPluginReload(::onload)
    }

    @EventHandler
    fun onServerLoad(event: ServerLoadEvent) {
        if (event.type == ServerLoadEvent.LoadType.STARTUP) onload()
    }

    fun onload() {
        config.load()
        if (!config.enabled) {
            spawnedNames.forEach { fpm.get(it)?.quit() }
            spawnedNames.clear()
            return
        }
        launch {
            val newNames = config.staticFakePlayerMetas.map { it.name.lowercase() }.toSet()
            withContext(plugin.dispatcher) {
                spawnedNames.removeIf { name ->
                    if (name.lowercase() !in newNames) {
                        fpm.get(name)?.quit()
                        true
                    } else {
                        false
                    }
                }
            }
            delay(config.delay)
            config.staticFakePlayerMetas.forEach { meta ->
                val exists = fpm.get(meta.name)
                val location = meta.location?.asLocation() ?: config.defaultLocation?.asLocation() ?: plugin.server.worlds.first().spawnLocation
                if (exists == null) {
                    val fakePlayer = fpm.spawn(
                        meta.name,
                        Bukkit.getConsoleSender(),
                        location
                    )
                    if (fakePlayer != null) {
                        spawnedNames.add(meta.name)
                        setupMeta(fakePlayer, meta)
                    }
                } else {
                    exists.player.teleportAsync(location)
                    setupMeta(exists, meta)
                }
            }
        }
    }

    suspend fun setupMeta(fakePlayer: FakePlayer, meta: Meta) = withContext(fakePlayer.dispatcher) {
        fakePlayer.ticking = meta.ticking ?: false
        if (fakePlayer.skin == null) fakePlayer.skin = SkinFetcher.getPlayerSkinInfoByName(meta.skin, true)
        fakePlayer.settings = meta.settings?.clone() ?: plugin.config.defaultSettings.clone()
    }

}