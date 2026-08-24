package com.coderxi.plugin.fakeplayer.utils

import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object ToolHelper {

    /**
     * 依據目標方塊種類，自動為玩家/假人從背包與快捷欄中裝備最適合且挖掘效率最高的工具
     */
    fun equipBestTool(player: Player, target: Block) {
        val blockType = target.type
        if (blockType.isAir) return
        val inv = player.inventory

        val preferredToolType = when {
            Tag.MINEABLE_PICKAXE.isTagged(blockType) -> "PICKAXE"
            Tag.MINEABLE_SHOVEL.isTagged(blockType) -> "SHOVEL"
            Tag.MINEABLE_AXE.isTagged(blockType) -> "AXE"
            Tag.MINEABLE_HOE.isTagged(blockType) -> "HOE"
            blockType.name.contains("LEAVES") || blockType.name == "COBWEB" || blockType.name.contains("WOOL") -> "SHEARS"
            else -> null
        }

        if (preferredToolType == null) return

        fun calculateScore(item: ItemStack?): Int {
            if (item == null || item.type.isAir) return 0
            val name = item.type.name
            val matchesType = when (preferredToolType) {
                "PICKAXE" -> name.endsWith("_PICKAXE")
                "SHOVEL" -> name.endsWith("_SHOVEL")
                "AXE" -> name.endsWith("_AXE")
                "HOE" -> name.endsWith("_HOE")
                "SHEARS" -> name == "SHEARS" || name.endsWith("_SWORD") || name.endsWith("_HOE")
                else -> false
            }
            if (!matchesType) return 0

            var tierScore = when {
                name.startsWith("NETHERITE_") -> 60
                name.startsWith("DIAMOND_") -> 50
                name.startsWith("IRON_") -> 40
                name.startsWith("GOLDEN_") -> 35
                name.startsWith("STONE_") -> 20
                name.startsWith("WOODEN_") -> 10
                name == "SHEARS" -> 50
                else -> 5
            }

            // 效率附魔加成
            val effLevel = item.getEnchantmentLevel(Enchantment.EFFICIENCY)
            tierScore += effLevel * 10
            return tierScore
        }

        val currentItem = inv.itemInMainHand
        val currentScore = calculateScore(currentItem)

        // 搜尋快捷欄 (0..8)
        var bestHotbarSlot = inv.heldItemSlot
        var maxHotbarScore = currentScore
        for (slot in 0..8) {
            val item = inv.getItem(slot)
            val score = calculateScore(item)
            if (score > maxHotbarScore) {
                maxHotbarScore = score
                bestHotbarSlot = slot
            }
        }

        // 搜尋背包主存儲區 (9..35)
        var bestInvSlot = -1
        var maxInvScore = maxHotbarScore
        for (slot in 9..35) {
            val item = inv.getItem(slot)
            val score = calculateScore(item)
            if (score > maxInvScore) {
                maxInvScore = score
                bestInvSlot = slot
            }
        }

        if (bestInvSlot != -1) {
            // 背包中有更好的工具，交換至當前快捷欄手持位置
            val currentSlot = inv.heldItemSlot
            val hotbarItem = inv.getItem(currentSlot)
            val invItem = inv.getItem(bestInvSlot)
            inv.setItem(currentSlot, invItem)
            inv.setItem(bestInvSlot, hotbarItem)
        } else if (bestHotbarSlot != inv.heldItemSlot && maxHotbarScore > currentScore) {
            // 快捷欄中有更適合的工具，直接切換快捷欄格位
            inv.heldItemSlot = bestHotbarSlot
        }
    }
}
