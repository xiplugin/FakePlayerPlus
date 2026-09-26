package com.coderxi.plugin.fakeplayer.api.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.model.ActionState
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID

interface FakePlayerManager {

    // 假人列表(仅在线)

    fun fakeplayers(): List<FakePlayer>

    fun fakeplayersCount(): Int

    fun fakeplayersByOwners(): Map<UUID, Collection<UUID>>

    fun fakeplayersByOwnerUuid(ownerUuid: UUID): Collection<FakePlayer>

    fun get(uuid: UUID): FakePlayer?

    fun get(name: String): FakePlayer?

    // 假人信息获取(数据库)

    suspend fun getFromRepository(uuid: UUID): FakePlayer?

    suspend fun getFromRepository(name: String): FakePlayer?

    suspend fun findNamesBySettingFromRepository(key: String, value: String): Collection<String>

    suspend fun getActiveActionStatesFromRepository(uuid: UUID): Collection<ActionState>

    suspend fun findNamesByCreatorUuidAndSetting(creatorUuid: UUID, settingKey: String, settingValue: String): Collection<String>

    // 假人判断(本地API)

    fun isNameUsed(name: String): Boolean

    // 假人判断(先在线 后数据库)

    fun isFake(uuid: UUID, queryFromRepository: Boolean = false): Boolean

    // 生成假人(先查数据库再生成)

    suspend fun spawn(name: String, spawner: CommandSender, location: Location? = null): FakePlayer?

    suspend fun rename(oldName: String, newName: String, operator: CommandSender, force: Boolean): FakePlayer

    suspend fun sequenceName(spawner: Player, reservedSequenceLength: Int = 1): String

    // 持久化假人信息(数据库)

    suspend fun saveSkin(fakePlayer: FakePlayer)

    suspend fun saveSettings(operator: CommandSender, fakePlayer: FakePlayer)

    suspend fun saveActions(uuid: UUID, actives: Collection<ActionState>)

    suspend fun addOwner(fakePlayer: FakePlayer, ownerUuid: UUID)

    suspend fun removeOwner(fakePlayer: FakePlayer, ownerUuid: UUID)

    // 导入假人信息(数据库)

    suspend fun importFakePlayerData(databaseFile: File, tableName: String): Int

}