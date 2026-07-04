package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionTrigger.Continuous
import com.coderxi.plugin.fakeplayer.api.action.ActionType
import com.coderxi.plugin.fakeplayer.api.action.JumpAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

object JumpProcessor : ActionProcessor<JumpAction> {

    override val supportedType get() = ActionType.JUMP

    override fun process(fakePlayer: FakePlayer, action: JumpAction, handler: ActionHandler) {
        if (action is Continuous) {
            fakePlayer.player.isJumping = true
        } else {
            fakePlayer.nms.jumpFromGround()
        }
    }

    override fun onStop(fakePlayer: FakePlayer, action: JumpAction) {
        fakePlayer.player.isJumping = false
    }
}
