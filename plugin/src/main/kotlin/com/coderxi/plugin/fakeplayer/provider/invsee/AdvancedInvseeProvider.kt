package com.coderxi.plugin.fakeplayer.provider.invsee

import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.utils.messages.tl
import com.coderxi.plugin.fakeplayer.utils.messages.tls
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

class AdvancedInvseeProvider: InvseeProvider {

    override fun openInventory(viewer: Player, whom: Player): InventoryView {
        return plugin.nms.openInventory(viewer, whom, false,  tl(viewer,"fakeplayer.inventory.title",whom.name)).view
    }

    override fun openEnderChest(viewer: Player, whom: Player): InventoryView? {
        return viewer.openInventory(whom.enderChest)?.apply {
            @Suppress("DEPRECATION")
            title = tls(viewer,"fakeplayer.enderchest.title",whom.name)
        }
    }
}
