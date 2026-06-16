package com.coderxi.plugin.fakeplayer.api.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface FakePlayerManager {

    // 假人列表
    fun fakeplayers(): Collection<FakePlayer>

    fun fakeplayersCount(): Int

    fun fakeplayersByOwnerUuid(ownerUuid: UUID): Collection<FakePlayer>

    fun get(uuid: UUID): FakePlayer?

    fun get(name: String): FakePlayer?

    fun isOwned(uniqueId: UUID, uuid: UUID): Boolean

    fun isOwned(player: Player, fakePlayer: FakePlayer) = isOwned(player.uniqueId, fakePlayer.uuid)

    // 操作假人
    fun spawnAsync(name: String, senderUuid: UUID, location: Location): CompletableFuture<FakePlayer>

    fun spawnAsync(name: String, sender: Player) = spawnAsync(name,sender.uniqueId, sender.location)

    fun remove(name: String, sender: Player)

    fun select(name: String, sender: Player): FakePlayer?

}