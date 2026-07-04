package com.coderxi.plugin.fakeplayer.api.nms

import org.bukkit.Server
import org.bukkit.entity.Player
import java.net.InetAddress
import java.util.UUID

interface NMSServer {

    val server: Server

    fun newPlayer(uuid: UUID, name: String): NMSServerPlayer

    fun placeNewPlayer(player: Player, address: InetAddress): NMSServerGamePacketListener

}
