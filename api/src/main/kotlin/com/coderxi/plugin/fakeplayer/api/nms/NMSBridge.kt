package com.coderxi.plugin.fakeplayer.api.nms

import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.net.InetAddress

interface NMSBridge {

    fun fromEntity(entity: Entity): NMSEntity

    fun fromServer(server: Server): NMSServer

    fun fromWorld(world: World): NMSServerLevel

    fun fromPlayer(player: Player): NMSServerPlayer

    fun createNetwork(address: InetAddress): NMSNetwork

}