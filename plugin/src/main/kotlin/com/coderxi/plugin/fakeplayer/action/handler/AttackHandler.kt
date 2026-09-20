package com.coderxi.plugin.fakeplayer.action.handler

import com.coderxi.plugin.fakeplayer.action.base.CommonActionHandler
import com.coderxi.plugin.fakeplayer.action.base.CommonActionMode
import com.coderxi.plugin.fakeplayer.action.type.AttackAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

object AttackHandler : CommonActionHandler<AttackAction>(
    type = AttackAction::class.java,
    name = "attack",
    modes = CommonActionMode.ALL
) {

    override fun runOnce(fakePlayer: FakePlayer, action: AttackAction) {
        val player = fakePlayer.player
        val target = player.rayTraceEntities(fakePlayer.nms.entityReachDistance.toInt())?.hitEntity
        player.swingMainHand()
        target?.let { player.attack(it) }
    }

    override fun runIntervalTick(fakePlayer: FakePlayer, action: AttackAction) {
        runOnce(fakePlayer, action)
    }

    override fun runContinuousTick(fakePlayer: FakePlayer, action: AttackAction) {
        runOnce(fakePlayer, action)
    }

}