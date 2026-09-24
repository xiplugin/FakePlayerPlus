package com.coderxi.plugin.fakeplayer.api.nms

import net.kyori.adventure.text.Component
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Player

interface NMSBridge {

    fun fromServer(server: Server): NMSServer

    fun fromWorld(world: World): NMSServerLevel

    fun fromPlayer(player: Player): NMSServerPlayer

    fun openInventory(viewer: Player, whom: Player, readOnly: Boolean, title: Component): NMSInventoryView

}