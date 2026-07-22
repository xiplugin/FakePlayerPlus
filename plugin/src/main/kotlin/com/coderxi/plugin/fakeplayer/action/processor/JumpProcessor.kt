package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionMode.Continuous
import com.coderxi.plugin.fakeplayer.api.action.JumpAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

object JumpProcessor : ActionProcessor<JumpAction> {

    override val actionType get() = JumpAction::class.java

    override fun process(fakePlayer: FakePlayer, action: JumpAction, handler: ActionHandler) {
        if (action.mode is Continuous) {
            fakePlayer.player.isJumping = true
        } else {
            fakePlayer.nms.jumpFromGround()
        }
    }

    override fun onStop(fakePlayer: FakePlayer, action: JumpAction) {
        fakePlayer.player.isJumping = false
    }
}
