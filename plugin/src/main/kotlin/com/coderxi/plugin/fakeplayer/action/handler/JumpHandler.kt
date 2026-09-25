package com.coderxi.plugin.fakeplayer.action.handler

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.JumpAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

object JumpHandler : CommonActionHandler<JumpAction>(
    type = JumpAction::class.java,
    name = "jump",
    modes = CommonActionMode.ALL
) {

    override fun runOnce(fakePlayer: FakePlayer, action: JumpAction) {
        fakePlayer.nms.jumpFromGround()
    }

    override fun runIntervalTick(fakePlayer: FakePlayer, action: JumpAction) {
        if (fakePlayer.nms.onGround) {
            fakePlayer.nms.jumpFromGround()
        }
    }

    override fun runContinuousTick(fakePlayer: FakePlayer, action: JumpAction) {
        fakePlayer.player.isJumping = true
    }

    override fun stopContinuous(fakePlayer: FakePlayer, action: JumpAction) {
        fakePlayer.player.isJumping = false
    }

}