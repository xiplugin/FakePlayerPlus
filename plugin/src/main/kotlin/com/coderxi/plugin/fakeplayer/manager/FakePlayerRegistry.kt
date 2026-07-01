package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakePlayerRegistry {

    internal val fakeplayers = ConcurrentHashMap<UUID, FakePlayer>()
    internal val fakeplayersByName = ConcurrentHashMap<String, FakePlayer>()
    internal val fakeplayersByOwnerUuids = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    private val writeLock = Any()
    @Volatile var sortedFakeplayers: List<FakePlayer> = emptyList(); private set

    fun register(fp: FakePlayer) = synchronized(writeLock) {
        fakeplayers[fp.uuid]?.let { oldFp ->
            oldFp.ownerUuids.forEach { oldOwnerId ->
                fakeplayersByOwnerUuids.computeIfPresent(oldOwnerId) { _, fpUuids ->
                    fpUuids.remove(fp.uuid)
                    if (fpUuids.isEmpty()) null else fpUuids
                }
            }
        }
        fakeplayers[fp.uuid] = fp
        fakeplayersByName[fp.name] = fp
        fp.ownerUuids.forEach { ownerId ->
            fakeplayersByOwnerUuids.computeIfAbsent(ownerId) { ConcurrentHashMap.newKeySet() }.add(fp.uuid)
        }
        sortedFakeplayers = fakeplayers.values.sortedBy { it.spawnTime }
    }

    fun unregister(uuid: UUID) = synchronized(writeLock) {
        fakeplayers.remove(uuid)?.let { fp ->
            fakeplayersByName.remove(fp.name)
            fp.ownerUuids.forEach { ownerId ->
                fakeplayersByOwnerUuids.computeIfPresent(ownerId) { _, fpUuids ->
                    fpUuids.remove(fp.uuid)
                    if (fpUuids.isEmpty()) null else fpUuids
                }
            }
            sortedFakeplayers = fakeplayers.values.sortedBy { it.spawnTime }
        }
    }

    fun fakeplayersByOwnerUuid(ownerId: UUID): Collection<FakePlayer> {
        return fakeplayersByOwnerUuids[ownerId]?.mapNotNull { fakeplayers[it] } ?: emptyList()
    }

}