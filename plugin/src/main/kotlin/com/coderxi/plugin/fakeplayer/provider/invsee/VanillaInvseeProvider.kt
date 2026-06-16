package com.coderxi.plugin.fakeplayer.provider.invsee

import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

class VanillaInvseeProvider: InvseeProvider, PluginComponent {

    override fun openInventory(viewer: Player, whom: Player): InventoryView?  {
        return viewer.openInventory(whom.inventory)?.apply {
            @Suppress("DEPRECATION")
            title = tls("fakeplayer.inventory.title",whom.name)
        }
    }

}