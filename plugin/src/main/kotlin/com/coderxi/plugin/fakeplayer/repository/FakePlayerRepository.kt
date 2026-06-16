package com.coderxi.plugin.fakeplayer.repository

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerPO
import org.sql2o.Connection
import java.util.UUID

class FakePlayerRepository : PluginComponent {

    fun open(): Connection = plugin.sql2o.open()

    fun findByUuid(uuid: UUID): FakePlayer? = open().use { conn ->
        val po = conn.createQuery("SELECT id, name, uuid, skin FROM fakeplayer WHERE uuid = :uuid LIMIT 1")
            .addParameter("uuid", uuid.toString())
            .executeAndFetch(FakePlayerPO::class.java)
            .firstOrNull() ?: return null
        mapToEntity(po, findOwnerUuidsById(conn, po.id))
    }

    fun findByName(name: String): FakePlayer? = open().use { conn ->
        val po = conn.createQuery("SELECT id, name, uuid, skin FROM fakeplayer WHERE LOWER(name) = LOWER(:name) LIMIT 1")
            .addParameter("name", name)
            .executeAndFetch(FakePlayerPO::class.java)
            .firstOrNull() ?: return null

        mapToEntity(po, findOwnerUuidsById(conn, po.id))
    }

    private fun findOwnerUuidsById(conn: Connection, fakePlayerId: Int): List<UUID> {
        return conn.createQuery("SELECT owner_uuid FROM ref_fakeplayer_owner WHERE fakeplayer_id = :playerId")
            .addParameter("playerId", fakePlayerId)
            .executeAndFetch(String::class.java)
            .map { UUID.fromString(it) }
    }

    private fun mapToEntity(po: FakePlayerPO, owners: List<UUID>): FakePlayer = StandardFakePlayer(
        name = po.name,
        uuid = UUID.fromString(po.uuid),
        skin = po.skin,
        ownerUuids = owners
    )

    fun save(fakePlayer: FakePlayer, updateOwners: Boolean = true) {
        val sql = "INSERT INTO fakeplayer (name, uuid, skin) VALUES (:name, :uuid, :skin)" +
                 "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, skin = excluded.skin"
        if (!updateOwners) {
            open().use { conn ->
                conn.createQuery(sql, false)
                    .addParameter("name", fakePlayer.name)
                    .addParameter("uuid", fakePlayer.uuid.toString())
                    .addParameter("skin", fakePlayer.skin)
                    .executeUpdate()
            }
            return
        }
        plugin.sql2o.beginTransaction().use { conn ->
            try {
                val fakePlayerId = conn.createQuery(sql, true)
                    .addParameter("name", fakePlayer.name)
                    .addParameter("uuid", fakePlayer.uuid.toString())
                    .addParameter("skin", fakePlayer.skin)
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

}