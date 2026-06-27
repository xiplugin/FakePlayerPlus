package com.coderxi.plugin.fakeplayer.nms.v26_2

import com.coderxi.plugin.fakeplayer.server.FakePlayerAdvancements
import org.bukkit.entity.Player
import java.nio.file.Paths

class NMSServerPlayerImpl(override val player: Player) : com.coderxi.plugin.fakeplayer.nms.v26_1_1.NMSServerPlayerImpl(player) {

    override fun disableAdvancements() {
        advancements = FakePlayerAdvancements(
            server.fixerUpper,
            server.playerList,
            server.advancements,
            Paths.get(System.getProperty("java.io.tmpdir")),
            handle
        )
    }

}