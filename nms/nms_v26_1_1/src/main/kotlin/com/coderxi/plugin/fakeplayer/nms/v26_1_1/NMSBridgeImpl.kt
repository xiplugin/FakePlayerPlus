package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.api.nms.NMSInventoryView
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import net.kyori.adventure.text.Component
import org.bukkit.Server
import org.bukkit.entity.Player

open class NMSBridgeImpl : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl() {

    override fun fromPlayer(player: Player): NMSServerPlayer = NMSServerPlayerImpl(player)

    override fun fromServer(server: Server): NMSServer = NMSServerImpl(server)

    override fun openInventory(viewer: Player, whom: Player, readOnly: Boolean, title: Component): NMSInventoryView = NMSInventoryViewImpl(viewer, whom, readOnly, title)

}