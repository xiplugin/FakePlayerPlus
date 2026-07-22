package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.AttackAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

object AttackProcessor : ActionProcessor<AttackAction> {

    override val actionType get() = AttackAction::class.java

    override fun process(fakePlayer: FakePlayer, action: AttackAction, handler: ActionHandler) {
        val player = fakePlayer.player
        val target = player.rayTraceEntities(fakePlayer.nms.entityReachDistance.toInt())?.hitEntity
        player.swingMainHand()
        target?.let { player.attack(it) }
    }

}
