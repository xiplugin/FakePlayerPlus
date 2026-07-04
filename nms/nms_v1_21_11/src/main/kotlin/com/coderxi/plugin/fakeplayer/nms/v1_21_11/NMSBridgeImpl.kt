package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerLevel
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Player

open class NMSBridgeImpl: NMSBridge {

    override fun fromServer(server: Server): NMSServer = NMSServerImpl(server)

    override fun fromWorld(world: World): NMSServerLevel = NMSServerLevelImpl(world)

    override fun fromPlayer(player: Player): NMSServerPlayer = NMSServerPlayerImpl(player)

}