package com.coderxi.plugin.fakeplayer.nms.v26_2

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

open class NMSServerPlayerImpl(override val player: Player) : com.coderxi.plugin.fakeplayer.nms.v26_1_1.NMSServerPlayerImpl(player) {

    override fun quit(cause: Component) {
        server.playerList.remove(handle,cause)
    }

}