package com.coderxi.plugin.fakeplayer.provider.invsee

import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

interface InvseeProvider {

    fun openInventory(viewer: Player, whom: Player): InventoryView?

    fun openEnderChest(viewer: Player, whom: Player): InventoryView?

    companion object : InvseeProvider,  PluginComponent {

        private var provider: InvseeProvider? = null

        private fun loadProvider(): InvseeProvider = synchronized(InvseeProvider) {
            provider = plugin.config.msic.invseeType.providerClass.getConstructor().newInstance()
            return provider!!
        }

        override fun onReload() {
            provider = null
        }

        override fun openInventory(viewer: Player, whom: Player): InventoryView? {
            return (provider ?: loadProvider()).openInventory(viewer, whom)
        }

        override fun openEnderChest(viewer: Player, whom: Player): InventoryView? {
            return (provider ?: loadProvider()).openEnderChest(viewer, whom)
        }

    }

}