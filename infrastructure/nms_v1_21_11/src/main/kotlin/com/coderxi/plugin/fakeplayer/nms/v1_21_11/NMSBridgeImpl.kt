package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSEntity
import com.coderxi.plugin.fakeplayer.api.nms.NMSNetwork
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerLevel
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.java.JavaPlugin
import java.net.InetAddress

class NMSBridgeImpl: NMSBridge {

    override fun supportVersion(): String  = "1.21.11"

    override fun fromEntity(entity: Entity): NMSEntity = NMSEntityImpl(entity)

    override fun fromServer(server: Server): NMSServer = NMSServerImpl(server)

    override fun fromWorld(world: World): NMSServerLevel = NMSServerLevelImpl(world)

    override fun fromPlayer(player: Player): NMSServerPlayer = NMSServerPlayerImpl(player)

    override fun createNetwork(address: InetAddress, plugin: JavaPlugin): NMSNetwork = NMSNetworkImpl(address,plugin)

}