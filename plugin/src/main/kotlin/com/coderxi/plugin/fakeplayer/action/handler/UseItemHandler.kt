package com.coderxi.plugin.fakeplayer.action.handler

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.UseItemAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.inventory.EquipmentSlot

object UseItemHandler : CommonActionHandler<UseItemAction>(
    type = UseItemAction::class.java,
    name = "use_item",
    modes = CommonActionMode.ALL
) {

    private fun runOnce0(fakePlayer: FakePlayer, action: UseItemAction): Boolean {
        if (fakePlayer.nms.isUsingItem) {
            return false
        }
        if (action.freezeTick > 0) {
            action.freezeTick--
            return false
        }
        val mainHandUsed = fakePlayer.nms.useItem(EquipmentSlot.HAND)
        val offHandUsed = fakePlayer.nms.useItem(EquipmentSlot.OFF_HAND)
        return mainHandUsed || offHandUsed
    }

    override fun runOnce(fakePlayer: FakePlayer, action: UseItemAction) {
        runOnce0(fakePlayer, action)
    }

    override fun runIntervalTick(fakePlayer: FakePlayer, action: UseItemAction) {
        runOnce0(fakePlayer, action)
    }

    override fun runContinuousTick(fakePlayer: FakePlayer, action: UseItemAction) {
        if (runOnce0(fakePlayer, action)) {
            action.freezeTick = 5
        }
    }

}