package com.coderxi.plugin.fakeplayer.repository.table

import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerPO
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerSettingsPO
import com.google.gson.Gson
import org.sql2o.Connection
import java.sql.SQLException
import java.util.UUID

class FakePlayerTable {

    val gson = Gson()
    val sql2o get() = plugin.sql2o

    fun findByUuid(uuid: UUID) = sql2o.open()
        .createQuery("SELECT id, name, uuid, creator_uuid AS creatorUuid, skin, settings FROM fakeplayer WHERE uuid = :uuid LIMIT 1")
        .addParameter("uuid", uuid.toString())
        .executeAndFetch(FakePlayerPO::class.java)
        .firstOrNull()

    fun findByName(name: String) = sql2o.open()
        .createQuery("SELECT id, name, uuid, creator_uuid AS creatorUuid, skin, settings FROM fakeplayer WHERE LOWER(name) = LOWER(:name) LIMIT 1")
        .addParameter("name", name)
        .executeAndFetch(FakePlayerPO::class.java)
        .firstOrNull()

    fun findOwnerUuidsByUuid(uuid: UUID) = sql2o.open()
        .createQuery("SELECT owner_uuid FROM ref_fakeplayer_owner WHERE fakeplayer_uuid = :fakePlayerUuid")
        .addParameter("fakePlayerUuid", uuid)
        .executeAndFetch(String::class.java)
        .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
        .toMutableSet()

    fun save(fakePlayerPO: FakePlayerPO, conn: Connection = sql2o.open()) {
        saveBatch(listOf(fakePlayerPO),conn)
    }

    fun save(fakePlayerPO: FakePlayerPO, ownerUuids: Collection<UUID>) = sql2o.beginTransaction().use { conn ->
        try {
            save(fakePlayerPO, conn)
            conn.createQuery("DELETE FROM ref_fakeplayer_owner WHERE fakeplayer_uuid = :fakePlayerUuid")
                .addParameter("fakePlayerUuid", fakePlayerPO.uuid)
                .executeUpdate()
            if (ownerUuids.isEmpty()) {
                conn.commit()
                return@use
            }
            conn.createQuery("INSERT INTO ref_fakeplayer_owner (fakeplayer_uuid, owner_uuid) VALUES (:fakePlayerUuid, :ownerUuid)").also { query ->
                ownerUuids.forEach { ownerUuid ->
                    query.addParameter("fakePlayerUuid", fakePlayerPO.uuid)
                        .addParameter("ownerUuid", ownerUuid)
                        .addToBatch()
                }
            }.executeBatch()
            conn.commit()
        } catch (e : Exception) {
            conn.rollback()
            throw e
        }
    }

    fun saveSkin(uuid: UUID, textures: PlayerTextures?) = sql2o.open()
        .createQuery("UPDATE fakeplayer SET skin = :skin WHERE uuid = :uuid")
        .addParameter("uuid", uuid.toString())
        .addParameter("skin", if (textures == null) null else "${textures.value}|${textures.signature}")
        .executeUpdate()
        .result > 0

    fun saveSettings(uuid: UUID, settingsPO: FakePlayerSettingsPO) = sql2o.open()
        .createQuery("UPDATE fakeplayer SET settings = :settings WHERE uuid = :uuid")
        .addParameter("uuid", uuid.toString())
        .addParameter("settings", gson.toJson(settingsPO))
        .executeUpdate()
        .result > 0

    fun saveBatch(fakePlayerPOs: Collection<FakePlayerPO>, conn : Connection = sql2o.open()) = conn
        .createQuery("INSERT INTO fakeplayer (name, uuid, creator_uuid, skin, settings) VALUES (:name, :uuid, :creatorUuid, :skin, :settings)" +
                "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, creator_uuid = excluded.creator_uuid, skin = excluded.skin, settings = excluded.settings")
        .also { query ->
            fakePlayerPOs.forEach { fakePlayerPO ->
                query.addParameter("name", fakePlayerPO.name)
                    .addParameter("uuid", fakePlayerPO.uuid)
                    .addParameter("creatorUuid", fakePlayerPO.creatorUuid)
                    .addParameter("skin", fakePlayerPO.skin)
                    .addParameter("settings", fakePlayerPO.settings)
                    .addToBatch()
            }
        }
        .executeBatch()
        .batchResult.sum()


    fun delete(uuid: UUID) {
        sql2o.beginTransaction().use { conn ->
            try {
                conn.createQuery("DELETE FROM fakeplayer WHERE uuid = :uuid")
                    .addParameter("uuid", uuid.toString())
                    .executeUpdate()

                conn.createQuery("DELETE FROM ref_fakeplayer_owner WHERE fakeplayer_uuid = :fakePlayerUuid")
                    .addParameter("fakePlayerUuid", uuid.toString())
                    .executeUpdate()
                conn.commit()
            } catch (e: SQLException) {
                conn.rollback()
                throw e
            }
        }
    }

    fun rename(oldUuid: UUID, newFakePlayerPO: FakePlayerPO) = sql2o.beginTransaction().use { conn ->
        val oldFakePlayerOwnerUuids = findOwnerUuidsByUuid(oldUuid)
        try {
            conn.createQuery("DELETE FROM fakeplayer WHERE uuid = :uuid")
                .addParameter("uuid", oldUuid.toString())
                .executeUpdate()
            save(newFakePlayerPO, conn)
            if (oldFakePlayerOwnerUuids.isEmpty()) {
                conn.commit()
                return@use
            }
            conn.createQuery("DELETE FROM ref_fakeplayer_owner WHERE fakeplayer_uuid = :fakePlayerUuid")
                .addParameter("fakePlayerUuid", oldUuid.toString())
                .executeUpdate()
            conn.createQuery("INSERT INTO ref_fakeplayer_owner (fakeplayer_uuid, owner_uuid) VALUES (:fakePlayerUuid, :ownerUuid)").also { query ->
                oldFakePlayerOwnerUuids.forEach { ownerUuid ->
                    query.addParameter("fakePlayerUuid", oldUuid)
                        .addParameter("ownerUuid", ownerUuid)
                        .addToBatch()
                }
            }.executeBatch()
            conn.commit()
        } catch (e: Exception) {
            conn.rollback()
            throw e
        }
    }

}