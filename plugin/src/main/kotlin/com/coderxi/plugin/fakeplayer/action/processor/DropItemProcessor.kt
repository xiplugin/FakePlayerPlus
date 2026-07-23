package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.DropItemAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

object DropItemProcessor : ActionProcessor<DropItemAction> {

    override val actionType get() = DropItemAction::class.java

    override fun process(fakePlayer: FakePlayer, action: DropItemAction, handler: ActionHandler) {
        fakePlayer.player.dropItem(false)
    }

}
