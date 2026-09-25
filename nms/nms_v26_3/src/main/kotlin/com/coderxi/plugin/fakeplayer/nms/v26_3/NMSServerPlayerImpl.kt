package com.coderxi.plugin.fakeplayer.nms.v26_3

import net.minecraft.world.InteractionHand
import net.minecraft.world.item.component.SwingAnimation
import org.bukkit.entity.Player

class NMSServerPlayerImpl(override val player: Player): com.coderxi.plugin.fakeplayer.nms.v26_1_1.NMSServerPlayerImpl(player) {

    override fun swingHand(hand: InteractionHand) {
        handle.swing(hand, SwingAnimation.DEFAULT, false)
    }

}