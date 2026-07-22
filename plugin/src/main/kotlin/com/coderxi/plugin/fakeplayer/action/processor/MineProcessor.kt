package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.MineAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer.BlockBreakActionType.*

object MineProcessor : ActionProcessor<MineAction> {

    override val actionType get() = MineAction::class.java

    override fun process(fakePlayer: FakePlayer, action: MineAction, handler: ActionHandler) {
        if (action.freezeTick > 0) { action.freezeTick--; return }
        val player = fakePlayer.player
        val maxDistance = fakePlayer.nms.blockReachDistance
        val target = player.rayTraceBlocks(maxDistance)?.hitBlock
        if (target == null || target.type.isAir) { resetMining(fakePlayer, action); return }
        player.swingMainHand()
        if (action.target == null || action.target != target) {
            if (action.target != null) resetMining(fakePlayer, action)
            fakePlayer.nms.doBlockBreakAction(target, START)
            action.target = target
            action.progress = 0f
        } else {
            action.progress += fakePlayer.nms.getDestroyProgress(target)
        }
        if (action.progress >= 1.0f) {
            fakePlayer.nms.doBlockBreakAction(target, STOP)
            resetMining(fakePlayer, action)
            action.freezeTick = 5
        }
    }

    private fun resetMining(fakePlayer: FakePlayer, action: MineAction) {
        val target = action.target ?: return
        fakePlayer.nms.doBlockBreakAction(target, ABORT)
        action.target = null
        action.progress = 0f
    }

    override fun onStop(fakePlayer: FakePlayer, action: MineAction) {
        resetMining(fakePlayer, action)
    }

}
