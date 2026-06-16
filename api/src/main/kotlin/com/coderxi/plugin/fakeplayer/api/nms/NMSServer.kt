package com.coderxi.plugin.fakeplayer.api.nms

import org.bukkit.Server
import java.util.UUID

interface NMSServer {

    val server: Server

    fun newPlayer(uuid: UUID, name: String): NMSServerPlayer

}
