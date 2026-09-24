package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSInventoryView
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerLevel
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import net.kyori.adventure.text.Component
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Player

open class NMSBridgeImpl: NMSBridge {

    override fun fromServer(server: Server): NMSServer = NMSServerImpl(server)

    override fun fromWorld(world: World): NMSServerLevel = NMSServerLevelImpl(world)

    override fun fromPlayer(player: Player): NMSServerPlayer = NMSServerPlayerImpl(player)

    override fun openInventory(viewer: Player, whom: Player, readOnly: Boolean, title: Component): NMSInventoryView = NMSInventoryViewImpl(viewer, whom, readOnly, title)
}
