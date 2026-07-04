package com.coderxi.plugin.fakeplayer.provider.invsee

import com.coderxi.plugin.fakeplayer.utils.onPluginReload
import com.coderxi.plugin.fakeplayer.utils.plugin
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

interface InvseeProvider {

    fun openInventory(viewer: Player, whom: Player): InventoryView?

    fun openEnderChest(viewer: Player, whom: Player): InventoryView?

    companion object {
        private var _current: InvseeProvider? = null

        val current: InvseeProvider get() = _current ?: plugin.config.behavior.invseeType.providerClass.getConstructor().newInstance().also { _current = it }

        init { onPluginReload { _current = null } }
    }

}