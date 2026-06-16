package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerPreparingEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerSpawnedEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.coderxi.plugin.fakeplayer.repository.FakePlayerRepository
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.UUID
import java.util.concurrent.CompletableFuture

class FakePlayerManagerImpl : FakePlayerManager, PluginComponent, Listener {

    val repository = FakePlayerRepository()
    val registry = FakePlayerRegistry()
    override fun fakeplayers() = registry.fakeplayers.values
    override fun fakeplayersCount() = registry.fakeplayers.count()
    override fun fakeplayersByOwnerUuid(ownerUuid: UUID) = registry.fakeplayersByOwnerUuid(ownerUuid)
    override fun get(uuid: UUID): FakePlayer? = registry.fakeplayers[uuid]
    override fun get(name: String): FakePlayer? = registry.fakeplayersByName[name]

    private fun uuid(name: String) = UUID.nameUUIDFromBytes("${plugin.name}:$name".toByteArray())

    override fun spawnAsync(name: String, senderUuid: UUID, location: Location): CompletableFuture<FakePlayer> {
        val fakePlayer = repository.findByName(name) ?: StandardFakePlayer(name,uuid(name),listOf(senderUuid)).apply(repository::save)
        FakePlayerPreparingEvent(fakePlayer).call()
        registry.register(fakePlayer)
        fakePlayer.connect(plugin.nms, plugin.nmsServer)
        FakePlayerConnectedEvent(fakePlayer).call()
        fakePlayer.setupDefaults()
        return fakePlayer.teleportAsync(location).thenApply { spawned ->
            if (!spawned) fakePlayer.quit("Spawn failed")
            FakePlayerSpawnedEvent(fakePlayer).call()
            fakePlayer
        }
    }

    override fun remove(name: String, sender: Player) {
        registry.fakeplayersByName[name]?.quit("Removed by " + sender.name)
    }

    override fun select(name: String, sender: Player): FakePlayer? {
        return null;
    }

    override fun isOwned(uniqueId: UUID, uuid: UUID): Boolean {
        return registry.fakeplayersByOwnerUuid(uniqueId).find{ it.uuid == uuid } != null
    }

}