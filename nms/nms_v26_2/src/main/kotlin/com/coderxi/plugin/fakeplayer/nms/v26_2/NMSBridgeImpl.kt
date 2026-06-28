package com.coderxi.plugin.fakeplayer.nms.v26_2

import org.bukkit.entity.Player

open class NMSBridgeImpl: com.coderxi.plugin.fakeplayer.nms.v26_1_1.NMSBridgeImpl() {

    override fun fromPlayer(player: Player) = NMSServerPlayerImpl(player)

}