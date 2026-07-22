package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionMode.Once
import com.coderxi.plugin.fakeplayer.api.action.SneakAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.plugin

object SneakProcessor : ActionProcessor<SneakAction> {

    override val actionType get() = SneakAction::class.java

    override fun process(fakePlayer: FakePlayer, action: SneakAction, handler: ActionHandler) {
        fakePlayer.player.isSneaking = !(action.mode is Once && fakePlayer.player.isSneaking)
    }

    override fun onStop(fakePlayer: FakePlayer, action: SneakAction) {
        if (action.mode !is Once) {
            fakePlayer.player.isSneaking = false
        } else {
            fakePlayer.player.scheduler.runDelayed(plugin,{
                fakePlayer.player.isSneaking = false
            }, null,2)
        }
    }

}
