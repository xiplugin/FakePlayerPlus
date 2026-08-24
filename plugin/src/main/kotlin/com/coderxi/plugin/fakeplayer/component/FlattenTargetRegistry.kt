package com.coderxi.plugin.fakeplayer.component

import org.bukkit.World
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class BlockKey(val worldName: String, val x: Int, val y: Int, val z: Int)

object FlattenTargetRegistry {

    private val blockReservations = ConcurrentHashMap<BlockKey, UUID>()
    private val playerReservations = ConcurrentHashMap<UUID, BlockKey>()

    fun reserve(world: World, x: Int, y: Int, z: Int, fakePlayerUuid: UUID): Boolean {
        val key = BlockKey(world.name, x, y, z)
        val existing = blockReservations[key]
        if (existing != null && existing != fakePlayerUuid) {
            return false
        }
        release(fakePlayerUuid)
        blockReservations[key] = fakePlayerUuid
        playerReservations[fakePlayerUuid] = key
        return true
    }

    fun release(fakePlayerUuid: UUID) {
        val oldKey = playerReservations.remove(fakePlayerUuid) ?: return
        blockReservations.remove(oldKey, fakePlayerUuid)
    }

    fun isReservedByOther(world: World, x: Int, y: Int, z: Int, fakePlayerUuid: UUID): Boolean {
        val key = BlockKey(world.name, x, y, z)
        val holder = blockReservations[key]
        return holder != null && holder != fakePlayerUuid
    }

    fun clearAll() {
        blockReservations.clear()
        playerReservations.clear()
    }
}
