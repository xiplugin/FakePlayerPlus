package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player as NMSPlayer0
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import org.bukkit.entity.Player

class NMSInventoryViewImpl(viewer: Player, whom: Player, readOnly: Boolean, title: Component) : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSInventoryViewImpl(viewer, whom, readOnly, title) {
    override fun containerMenu(containerId: Int, viewer: ServerPlayer, whom: ServerPlayer, readOnly: Boolean, title: Component): AbstractContainerMenu {
        return object : BaseInventoryMenu(containerId,viewer, whom, readOnly, title) {
            override fun clicked(slotId: Int, button: Int, containerInput: ContainerInput, player: NMSPlayer0) {
                if (intercept(slotId, containerInput == ContainerInput.QUICK_CRAFT)) return
                super.clicked(slotId, button, containerInput, player)
            }
        }
    }
}