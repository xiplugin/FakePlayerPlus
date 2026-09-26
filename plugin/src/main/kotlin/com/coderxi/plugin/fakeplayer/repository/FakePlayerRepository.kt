package com.coderxi.plugin.fakeplayer.repository

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.model.ActionState
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerActionsPO
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerPO
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerSettingsPO
import com.coderxi.plugin.fakeplayer.repository.table.FakePlayerTable
import com.coderxi.plugin.fakeplayer.utils.common.Sql2oUtil
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sql2o.Sql2o
import java.io.File
import java.util.UUID

class FakePlayerRepository {

    private val gson = Gson()

    private val table = FakePlayerTable()

    fun findByUuid(uuid: UUID): StandardFakePlayer? {
        return table.findByUuid(uuid)?.toEntity(table.findOwnerUuidsByUuid(uuid))
    }

    fun findByName(name: String): StandardFakePlayer? {
        return table.findByName(name)?.let { it.toEntity(table.findOwnerUuidsByUuid(UUID.fromString(it.uuid))) }
    }

    fun findNamesBySetting(key: String, value: String): Collection<String> {
        return table.findNamesBySetting(key, value)
    }

    fun findActiveActionStatesByUuid(uuid: UUID): Collection<ActionState> {
        return gson.fromJson(table.findByUuid(uuid)?.actions?:"{}", FakePlayerActionsPO::class.java).actives ?: emptyList()
    }


    fun findNamesByCreatorUuidAndSetting(creatorUuid: UUID, settingKey: String, settingValue: String): Collection<String> {
        return table.findNamesByCreatorUuidAndSetting(creatorUuid,settingKey,settingValue)
    }

    fun save(fakePlayer: FakePlayer, saveOwners: Boolean) {
        val fakePlayerPO = FakePlayerPO.fromEntity(fakePlayer)
        if (saveOwners) {
            table.save(fakePlayerPO, fakePlayer.owners.map { it.uuid }.toSet())
        } else {
            table.save(fakePlayerPO)
        }
    }

    fun delete(uuid: UUID) {
        table.delete(uuid)
    }

    fun rename(oldUuid: UUID, newFakePlayer: StandardFakePlayer) {
        return table.rename(oldUuid, FakePlayerPO.fromEntity(newFakePlayer))
    }

    fun saveSkin(fakePlayer: FakePlayer): Boolean {
        return table.saveSkin(fakePlayer.uuid, fakePlayer.textures)
    }

    fun saveSettings(fakePlayer: FakePlayer): Boolean {
        return table.saveSettings(fakePlayer.uuid, FakePlayerSettingsPO.fromEntity(fakePlayer.settings))
    }

    fun saveActions(uuid: UUID, actives: Collection<ActionState>): Boolean {
        return table.saveActions(uuid, FakePlayerActionsPO(actives))
    }

    suspend fun importFakePlayerData(databaseFile: File, tableName: String): Int {
        val fakePlayerPOs = withContext(Dispatchers.IO) {
            val database = Sql2o("jdbc:sqlite:${databaseFile.absolutePath}", null, null)
            val tableExists = Sql2oUtil.checkTableExists(database.open(), tableName)
            if (!tableExists) throw FakePlayerCommandException.NoSuchTableException(tableName)
            database.open().createQuery("SELECT * FROM $tableName").executeAndFetch(FakePlayerPO::class.java)
        }
        return table.saveBatch(fakePlayerPOs)
    }


}