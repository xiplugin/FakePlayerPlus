package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import org.bukkit.entity.Player
import java.net.InetAddress

open class NMSBridgeImpl : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl(), NMSBridge {

    override fun createNetwork(address: InetAddress) = NMSNetworkImpl(address)

    override fun fromPlayer(player: Player): NMSServerPlayer = NMSServerPlayerImpl(player)

}