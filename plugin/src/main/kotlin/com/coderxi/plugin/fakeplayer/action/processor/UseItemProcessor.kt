package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionMode.*
import com.coderxi.plugin.fakeplayer.api.action.UseItemAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import org.bukkit.inventory.EquipmentSlot

object UseItemProcessor : ActionProcessor<UseItemAction> {

    override val actionType get() = UseItemAction::class.java

    override fun process(fakePlayer: FakePlayer, action: UseItemAction, handler: ActionHandler) {
        if (fakePlayer.nms.isUsingItem) return
        if (action.freezeTick > 0) { action.freezeTick--; return }
        val mainHandUsed = fakePlayer.nms.useItem(EquipmentSlot.HAND)
        val offHandUsed = fakePlayer.nms.useItem(EquipmentSlot.OFF_HAND)
        if ((mainHandUsed || offHandUsed) && action.mode is Continuous) { action.freezeTick = 5 }
    }

    override fun onStop(fakePlayer: FakePlayer, action: UseItemAction) {
        fakePlayer.nms.releaseUsingItem()
    }

}