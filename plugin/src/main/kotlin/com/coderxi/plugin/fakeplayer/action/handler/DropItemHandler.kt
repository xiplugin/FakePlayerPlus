package com.coderxi.plugin.fakeplayer.action.handler

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.DropItemAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

object DropItemHandler : CommonActionHandler<DropItemAction>(
    type = DropItemAction::class.java,
    name = "drop_item",
    modes = CommonActionMode.ALL
) {

    override fun runOnce(fakePlayer: FakePlayer, action: DropItemAction) {
        fakePlayer.player.dropItem(false)
    }

    override fun runIntervalTick(fakePlayer: FakePlayer, action: DropItemAction) {
        runOnce(fakePlayer, action)
    }

    override fun runContinuousTick(fakePlayer: FakePlayer, action: DropItemAction) {
        runOnce(fakePlayer, action)
    }
}