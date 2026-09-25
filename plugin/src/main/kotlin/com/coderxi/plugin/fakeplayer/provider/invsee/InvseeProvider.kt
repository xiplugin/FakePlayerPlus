package com.coderxi.plugin.fakeplayer.provider.invsee

import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

interface InvseeProvider {

    fun openInventory(viewer: Player, whom: Player): InventoryView?

    fun openEnderChest(viewer: Player, whom: Player): InventoryView?

    companion object : PluginComponent {

        private var _current: InvseeProvider? = null

        val current: InvseeProvider get() = _current ?: plugin.config.msic.invseeType.providerClass.getConstructor().newInstance().also { _current = it }

        override fun onReload() {
            _current = null
        }
    }

}