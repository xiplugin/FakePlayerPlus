package com.coderxi.plugin.fakeplayer.api.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

interface FakePlayerManager {

    // 假人列表(仅在线)

    fun fakeplayers(): Collection<FakePlayer>

    fun fakeplayersCount(): Int

    fun fakeplayersByOwners(): Map<UUID, Collection<UUID>>

    fun fakeplayersByOwnerUuid(ownerUuid: UUID): Collection<FakePlayer>

    fun get(uuid: UUID): FakePlayer?

    fun get(name: String): FakePlayer?

    // 假人信息获取(数据库)

    suspend fun getFromRepository(uuid: UUID): FakePlayer?

    suspend fun getFromRepository(name: String): FakePlayer?

    // 假人判断(本地API)

    fun isNameUsed(name: String): Boolean

    // 生成假人(先查数据库再生成)

    suspend fun spawn(name: String, spawner: CommandSender, location: Location? = null): FakePlayer?

    suspend fun sequenceName(spawner: Player, reservedSequenceLength: Int = 1): String

    // 持久化假人信息(数据库)

    suspend fun saveSkin(fakePlayer: FakePlayer)

    suspend fun saveSettings(fakePlayer: FakePlayer)

    suspend fun addOwner(fakePlayer: FakePlayer, ownerUuid: UUID)

    suspend fun removeOwner(fakePlayer: FakePlayer, ownerUuid: UUID)

}