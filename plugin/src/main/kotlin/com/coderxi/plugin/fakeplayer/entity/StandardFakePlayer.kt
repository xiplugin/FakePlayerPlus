package com.coderxi.plugin.fakeplayer.entity

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.*
import com.coderxi.plugin.fakeplayer.utils.IPGenerator
import java.util.UUID

class StandardFakePlayer(
    override val name: String,
    override val uuid: UUID,
    override var ownerUuids: Collection<UUID> = emptyList(),
    override var skin: String? = null,
) : FakePlayer {

    override lateinit var nmsPlayer: NMSServerPlayer
    override lateinit var nmsNetwork: NMSNetwork
    override lateinit var nmsConnection: NMSServerGamePacketListener

    override fun connect(nms: NMSBridge, nmsServer: NMSServer) {
        nmsPlayer = nmsServer.newPlayer(uuid, name)
        nmsPlayer.disableAdvancements()
        nmsNetwork = nms.createNetwork(IPGenerator.next())
        nmsConnection = nmsNetwork.placeNewPlayer(player)
        nmsConnection.ping = -1
    }

    override fun setupDefaults() {
        player.apply {
            isPersistent = true
            isSleepingIgnored = true
            isInvulnerable = false
            isCollidable = true
            canPickupItems = true
            health = 20.0
            foodLevel = 20
        }
    }

}