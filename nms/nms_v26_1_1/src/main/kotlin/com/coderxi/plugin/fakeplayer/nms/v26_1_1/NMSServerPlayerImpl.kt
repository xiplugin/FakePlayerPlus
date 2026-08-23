package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.EntityHitResult
import org.bukkit.block.Block
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.entity.Player

open class NMSServerPlayerImpl(override val player: Player) : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSServerPlayerImpl(player) {

    override fun getDestroyProgress(target: Block): Float {
        val block = target as CraftBlock
        return block.blockState.getDestroyProgress(handle,handle.level(), block.position)
    }

    override fun useItemOnEntity(level: ServerLevel, stack: ItemStack, hand: InteractionHand, entityHitResult: EntityHitResult): Boolean {
        val entity = entityHitResult.entity
        val relativePos = entityHitResult.location.subtract(entity.x, entity.y, entity.z)
        if (entity.interact(handle, hand, relativePos).consumesAction()) {
            handle.swing(hand)
            return true
        }
        if (handle.interactOn(entity, hand, relativePos).consumesAction()) {
            handle.swing(hand)
            return true
        }
        return false
    }
}