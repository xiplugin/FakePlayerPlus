package com.coderxi.plugin.fakeplayer.repository

import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerPO
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sql2o.Connection
import org.sql2o.Sql2o
import java.io.File
import java.util.UUID

class FakePlayerRepository {

    fun open(): Connection = plugin.sql2o.open()

    private val gson = Gson()

    fun findByUuid(uuid: UUID): FakePlayer? = open().use { conn ->
        val po = conn.createQuery("SELECT id, name, uuid, creator_uuid AS creatorUuid, skin, settings FROM fakeplayer WHERE uuid = :uuid LIMIT 1")
            .addParameter("uuid", uuid.toString())
            .executeAndFetch(FakePlayerPO::class.java)
            .firstOrNull() ?: return null
        mapToEntity(po, findOwnerUuidsByUuid(conn, po.uuid))
    }

    fun findByName(name: String): FakePlayer? = open().use { conn ->
        val po = conn.createQuery("SELECT id, name, uuid, creator_uuid AS creatorUuid, skin, settings FROM fakeplayer WHERE LOWER(name) = LOWER(:name) LIMIT 1")
            .addParameter("name", name)
            .executeAndFetch(FakePlayerPO::class.java)
            .firstOrNull() ?: return null

        mapToEntity(po, findOwnerUuidsByUuid(conn, po.uuid))
    }

    private fun findOwnerUuidsByUuid(conn: Connection, fakePlayerUuid: String): MutableSet<UUID> {
        return conn.createQuery("SELECT owner_uuid FROM ref_fakeplayer_owner WHERE fakeplayer_uuid = :fakePlayerUuid")
            .addParameter("fakePlayerUuid", fakePlayerUuid)
            .executeAndFetch(String::class.java)
            .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            .toMutableSet()
    }

    private fun mapToEntity(po: FakePlayerPO, owners: MutableSet<UUID>): FakePlayer {
        val skinSplit = po.skin?.split("|")
        val skin = if (skinSplit != null && skinSplit .size > 1) { FakePlayer.SkinInfo(skinSplit[0],skinSplit[1]) } else null
        val settings = if (po.settings != null) gson.fromJson(po.settings, FakePlayerSettings::class.java) else plugin.config.defaultSettings.clone()
        return StandardFakePlayer(po.name, UUID.fromString(po.uuid), runCatching { UUID.fromString(po.creatorUuid) }.getOrNull() ,owners, skin, settings)
    }

    fun save(fakePlayer: FakePlayer, saveOwners: Boolean) {
        val sql = "INSERT INTO fakeplayer (name, uuid, creator_uuid, skin, settings) VALUES (:name, :uuid, :creatorUuid, :skin, :settings)" +
                 "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, creator_uuid = excluded.creator_uuid, skin = excluded.skin, settings = excluded.settings"
        if (!saveOwners) {
            open().use { conn ->
                conn.createQuery(sql, false)
                    .addParameter("name", fakePlayer.name)
                    .addParameter("uuid", fakePlayer.uuid.toString())
                    .addParameter("creatorUuid", fakePlayer.creatorUuid?.toString())
                    .addParameter("skin", if (fakePlayer.skin == null) null else "${fakePlayer.skin!!.textures}|${fakePlayer.skin!!.signature}")
                    .addParameter("settings", if (plugin.config.defaultSettings.equals2(fakePlayer.settings)) null else gson.toJson(fakePlayer.settings))
                    .executeUpdate()
            }
            return
        }
        plugin.sql2o.beginTransaction().use { conn ->
            try {
                conn.createQuery(sql)
                    .addParameter("name", fakePlayer.name)
                    .addParameter("uuid", fakePlayer.uuid.toString())
                    .addParameter("creatorUuid", fakePlayer.creatorUuid?.toString())
                    .addParameter("skin", if (fakePlayer.skin == null) null else "${fakePlayer.skin!!.textures}|${fakePlayer.skin!!.signature}")
                    .addParameter("settings", if (plugin.config.defaultSettings.equals2(fakePlayer.settings)) null else gson.toJson(fakePlayer.settings))
                    .executeUpdate()

                conn.createQuery("DELETE FROM ref_fakeplayer_owner WHERE fakeplayer_uuid = :fakePlayerUuid")
                    .addParameter("fakePlayerUuid", fakePlayer.uuid.toString())
                    .executeUpdate()

                if (fakePlayer.ownerUuids.isNotEmpty()) {
                    val batchQuery = conn.createQuery("INSERT INTO ref_fakeplayer_owner (fakeplayer_uuid, owner_uuid) VALUES (:fakePlayerUuid, :ownerUuid)")
                    for (ownerUuid in fakePlayer.ownerUuids) {
                        batchQuery.addParameter("fakePlayerUuid", fakePlayer.uuid.toString())
                            .addParameter("ownerUuid", ownerUuid.toString())
                            .addToBatch()
                    }
                    batchQuery.executeBatch()
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }

    }

    fun saveSkin(fakePlayer: FakePlayer) {
        val sql = "UPDATE fakeplayer SET skin = :skin WHERE uuid = :uuid"
        open().use { conn ->
            conn.createQuery(sql)
                .addParameter("uuid", fakePlayer.uuid.toString())
                .addParameter("skin", if (fakePlayer.skin == null) null else "${fakePlayer.skin!!.textures}|${fakePlayer.skin!!.signature}")
                .executeUpdate()
        }
    }

    fun saveSettings(fakePlayer: FakePlayer) {
        val sql = "UPDATE fakeplayer SET settings = :settings WHERE uuid = :uuid"
        open().use { conn ->
            conn.createQuery(sql)
                .addParameter("uuid", fakePlayer.uuid.toString())
                .addParameter("settings", if (plugin.config.defaultSettings.equals2(fakePlayer.settings)) null else gson.toJson(fakePlayer.settings))
                .executeUpdate()
        }
    }

    suspend fun importFakePlayerData(databaseFile: File, tableName: String) : Int {
        val fakePlayers = withContext(Dispatchers.IO) {
            val database = Sql2o("jdbc:sqlite:${databaseFile.absolutePath}", null, null)
            database.open().use { conn1 ->
                val tableExists = conn1.createQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name=:name").addParameter("name", tableName).executeScalar(Int::class.java) > 0
                if (!tableExists) {
                    throw FakePlayerCommandException.NoSuchTableException(tableName)
                }
                @Suppress("SqlSourceToSinkFlow")
                conn1.createQuery("SELECT * FROM $tableName").executeAndFetch(FakePlayerPO::class.java)
            }
        }
        return insertFakePlayers(fakePlayers)
    }

    private fun insertFakePlayers(fakePlayers: Collection<FakePlayerPO>): Int {
        val sql = "INSERT INTO fakeplayer (name, uuid, creator_uuid, skin, settings) VALUES (:name, :uuid, :creatorUuid, :skin, :settings)" +
                "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, creator_uuid = excluded.creator_uuid, skin = excluded.skin, settings = excluded.settings"
        return open().use { conn ->
            val query = conn.createQuery(sql)
            for (player in fakePlayers) {
                query.addParameter("name", player.name)
                    .addParameter("uuid", player.uuid)
                    .addParameter("creatorUuid", player.creatorUuid)
                    .addParameter("skin", player.skin)
                    .addParameter("settings", player.settings)
                    .addToBatch()
            }
            query.executeBatch().batchResult.sum()
        }
    }


}