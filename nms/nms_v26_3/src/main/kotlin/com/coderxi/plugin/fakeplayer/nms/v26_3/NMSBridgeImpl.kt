package com.coderxi.plugin.fakeplayer.nms.v26_3

import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import org.bukkit.entity.Player

class NMSBridgeImpl : com.coderxi.plugin.fakeplayer.nms.v26_1_1.NMSBridgeImpl() {

    override fun fromPlayer(player: Player): NMSServerPlayer = NMSServerPlayerImpl(player)

}