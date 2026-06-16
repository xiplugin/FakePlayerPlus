package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakePlayerRegistry {

    internal val fakeplayers = ConcurrentHashMap<UUID, FakePlayer>()
    internal val fakeplayersByName = ConcurrentHashMap<String, FakePlayer>()
    internal val fakeplayersByOwnerUuids = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    fun register(fp: FakePlayer) {
        fakeplayers[fp.uuid] = fp
        fakeplayersByName[fp.name] = fp
        fp.ownerUuids.forEach { ownerId ->
            fakeplayersByOwnerUuids.computeIfAbsent(ownerId) { ConcurrentHashMap.newKeySet() }.add(fp.uuid)
        }
    }

    fun unregister(uuid: UUID) = fakeplayers[uuid]?.let { fp ->
        fakeplayers.remove(fp.uuid)
        fakeplayersByName.remove(fp.name)
        fp.ownerUuids.forEach { ownerId ->
            fakeplayersByOwnerUuids[ownerId]?.let { fpUuids ->
                fpUuids.remove(fp.uuid)
                if (fpUuids.count() <= 0) fakeplayersByOwnerUuids.remove(ownerId)
            }
        }
    }

    fun fakeplayersByOwnerUuid(ownerId: UUID): Collection<FakePlayer> {
        return fakeplayersByOwnerUuids[ownerId]?.mapNotNull { fakeplayers[it] } ?: emptyList()
    }

}