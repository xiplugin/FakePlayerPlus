package com.coderxi.plugin.fakeplayer.action.handler

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.SneakAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.plugin

object SneakHandler : CommonActionHandler<SneakAction>(
    type = SneakAction::class.java,
    name = "sneak",
    modes = setOf(CommonActionMode.ONCE, CommonActionMode.CONTINUOUS)
) {

    override fun runOnce(fakePlayer: FakePlayer, action: SneakAction) {
        fakePlayer.player.isSneaking = true
    }

    override fun stopOnce(fakePlayer: FakePlayer, action: SneakAction) {
        fakePlayer.player.scheduler.runDelayed(plugin,{
            fakePlayer.player.isSneaking = false
        }, null,2)
    }

    override fun runIntervalTick(fakePlayer: FakePlayer, action: SneakAction) {
        throw UnsupportedOperationException()
    }

    override fun runContinuousTick(fakePlayer: FakePlayer, action: SneakAction) {
        fakePlayer.player.isSneaking = true
    }

    override fun stopContinuous(fakePlayer: FakePlayer, action: SneakAction) {
        fakePlayer.player.isSneaking = false
    }

}