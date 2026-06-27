package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import org.bukkit.block.Block
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.entity.Player

open class NMSServerPlayerImpl(override val player: Player) : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSServerPlayerImpl(player) {

    override fun getDestroyProgress(target: Block): Float {
        val block = target as CraftBlock
        return block.blockState.getDestroyProgress(handle,handle.level(), block.position)
    }

}