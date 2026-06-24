package com.coderxi.plugin.fakeplayer.repository

import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerPO
import com.google.gson.Gson
import org.sql2o.Connection
import java.util.UUID

class FakePlayerRepository : PluginComponent {

    fun open(): Connection = plugin.sql2o.open()

    private val gson = Gson()

    fun findByUuid(uuid: UUID): FakePlayer? = open().use { conn ->
        val po = conn.createQuery("SELECT id, name, uuid, creator_uuid AS creatorUuid, skin, settings FROM fakeplayer WHERE uuid = :uuid LIMIT 1")
            .addParameter("uuid", uuid.toString())
            .executeAndFetch(FakePlayerPO::class.java)
            .firstOrNull() ?: return null
        mapToEntity(po, findOwnerUuidsById(conn, po.id))
    }

    fun findByName(name: String): FakePlayer? = open().use { conn ->
        val po = conn.createQuery("SELECT id, name, uuid, creator_uuid AS creatorUuid, skin, settings FROM fakeplayer WHERE LOWER(name) = LOWER(:name) LIMIT 1")
            .addParameter("name", name)
            .executeAndFetch(FakePlayerPO::class.java)
            .firstOrNull() ?: return null

        mapToEntity(po, findOwnerUuidsById(conn, po.id))
    }

    private fun findOwnerUuidsById(conn: Connection, fakePlayerId: Int): MutableSet<UUID> {
        return conn.createQuery("SELECT owner_uuid FROM ref_fakeplayer_owner WHERE fakeplayer_id = :playerId")
            .addParameter("playerId", fakePlayerId)
            .executeAndFetch(String::class.java)
            .map { UUID.fromString(it) }
            .toMutableSet()
    }

    private fun mapToEntity(po: FakePlayerPO, owners: MutableSet<UUID>): FakePlayer {
        val skinSplit = po.skin?.split("|")
        val skin = if (skinSplit != null && skinSplit .size > 1) { FakePlayer.SkinInfo(skinSplit[0],skinSplit[1]) } else null
        val settings = if (po.settings != null) gson.fromJson(po.settings, FakePlayerSettings::class.java) else plugin.config.defaultSettings.clone()
        return StandardFakePlayer(po.name, UUID.fromString(po.uuid), po.creatorUuid,owners, skin, settings)
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
                val fakePlayerId = conn.createQuery(sql, true)
                    .addParameter("name", fakePlayer.name)
                    .addParameter("uuid", fakePlayer.uuid.toString())
                    .addParameter("creatorUuid", fakePlayer.creatorUuid?.toString())
                    .addParameter("skin", if (fakePlayer.skin == null) null else "${fakePlayer.skin!!.textures}|${fakePlayer.skin!!.signature}")
                    .addParameter("settings", if (plugin.config.defaultSettings.equals2(fakePlayer.settings)) null else gson.toJson(fakePlayer.settings))
                    .executeUpdate()
                    .getKey()
                conn.createQuery("DELETE FROM ref_fakeplayer_owner WHERE fakeplayer_id = :playerId")
                    .addParameter("playerId", fakePlayerId)
                    .executeUpdate()
                if (fakePlayer.ownerUuids.isNotEmpty()) {
                    val batchQuery = conn.createQuery("INSERT INTO ref_fakeplayer_owner (fakeplayer_id, owner_uuid) VALUES (:playerId, :ownerUuid)")
                    for (ownerUuid in fakePlayer.ownerUuids) {
                        batchQuery.addParameter("playerId", fakePlayerId)
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

    fun saveSkin(fakePlayer: FakePlayer){
        val sql = "UPDATE fakeplayer SET skin = :skin WHERE uuid = :uuid"
        open().use { conn ->
            val affectedRows = conn.createQuery(sql)
                .addParameter("uuid", fakePlayer.uuid.toString())
                .addParameter("skin", if (fakePlayer.skin == null) null else "${fakePlayer.skin!!.textures}|${fakePlayer.skin!!.signature}")
                .executeUpdate()
                .result
            affectedRows > 0
        }
    }

    fun saveSettings(fakePlayer: FakePlayer) {
        val sql = "UPDATE fakeplayer SET settings = :settings WHERE uuid = :uuid"
        open().use { conn ->
            val affectedRows = conn.createQuery(sql)
                .addParameter("uuid", fakePlayer.uuid.toString())
                .addParameter("settings", if (plugin.config.defaultSettings.equals2(fakePlayer.settings)) null else gson.toJson(fakePlayer.settings))
                .executeUpdate()
                .result
            affectedRows > 0
        }
    }


}