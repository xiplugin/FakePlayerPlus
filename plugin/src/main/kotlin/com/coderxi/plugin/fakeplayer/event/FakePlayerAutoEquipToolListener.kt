package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent

class FakePlayerAutoEquipToolListener(private val fpm: FakePlayerManager) : Listener {

    @EventHandler
    fun onBlockDamage(event: BlockDamageEvent) {
        val fakePlayer = fpm.get(event.player.uniqueId)?.takeIf { it.autoEquipTool } ?: return
        val inventory = fakePlayer.player.inventory
        val currentSlot = inventory.heldItemSlot
        val bestToolSlot = fakePlayer.nms.findBestToolSlot(event.block) ?: return
        if (bestToolSlot == currentSlot) return
        //将背包物品交换到主手
        val currentItem = inventory.getItem(currentSlot)
        val bestTool = inventory.getItem(bestToolSlot)
        inventory.setItem(currentSlot, bestTool)
        inventory.setItem(bestToolSlot, currentItem)
    }

}