package com.coderxi.plugin.fakeplayer.api.nms

import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Player

interface NMSBridge {

    fun fromServer(server: Server): NMSServer

    fun fromWorld(world: World): NMSServerLevel

    fun fromPlayer(player: Player): NMSServerPlayer

}