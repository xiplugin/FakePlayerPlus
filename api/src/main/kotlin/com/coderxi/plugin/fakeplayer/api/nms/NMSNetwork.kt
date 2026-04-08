package com.coderxi.plugin.fakeplayer.api.nms

import org.bukkit.entity.Player

interface NMSNetwork {

    fun placeNewPlayer(player: Player): NMSServerGamePacketListener

}
