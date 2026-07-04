package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import org.bukkit.Server
import org.bukkit.entity.Player

open class NMSBridgeImpl : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl() {

    override fun fromPlayer(player: Player): NMSServerPlayer = NMSServerPlayerImpl(player)

    override fun fromServer(server: Server): NMSServer = NMSServerImpl(server)

}