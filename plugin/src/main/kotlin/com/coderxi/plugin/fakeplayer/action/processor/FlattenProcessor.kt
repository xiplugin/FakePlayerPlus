package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionTrack
import com.coderxi.plugin.fakeplayer.api.action.FlattenAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer.BlockBreakActionType.*
import com.coderxi.plugin.fakeplayer.utils.tlp
import org.bukkit.block.Block

object FlattenProcessor : ActionProcessor<FlattenAction> {

    override val actionType get() = FlattenAction::class.java

    override fun process(fakePlayer: FakePlayer, action: FlattenAction, handler: ActionHandler) {
        if (action.freezeTick > 0) { action.freezeTick--; return }
        val player = fakePlayer.player
        val world = action.world ?: player.world

        // 尋找或驗證目標方塊
        var target = action.target
        if (target == null || target.type.isAir || target.world != world) {
            target = findNextBlock(world, action)
            if (target == null) {
                // 選區內方塊已全數清空
                resetMining(fakePlayer, action)
                fakePlayer.owners.forEach {
                    it.sendMessage(tlp("fakeplayer.flatten.complete", fakePlayer.name))
                }
                handler.stop(ActionTrack.INTERACTION)
                return
            }
        }

        // 調整假人視線朝向目標方塊
        val targetCenter = target.location.add(0.5, 0.5, 0.5)
        val eyeLoc = player.eyeLocation
        val direction = targetCenter.toVector().subtract(eyeLoc.toVector())
        if (direction.lengthSquared() > 0.0001) {
            player.teleport(player.location.setDirection(direction.normalize()))
        }

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
            action.freezeTick = 3
        }
    }

    private fun findNextBlock(world: org.bukkit.World, action: FlattenAction): Block? {
        // 由上往下（maxY -> minY）優先搜尋
        for (y in action.maxY downTo action.minY) {
            for (x in action.minX..action.maxX) {
                for (z in action.minZ..action.maxZ) {
                    val block = world.getBlockAt(x, y, z)
                    if (!block.type.isAir && block.type.isSolid) {
                        return block
                    }
                }
            }
        }
        return null
    }

    private fun resetMining(fakePlayer: FakePlayer, action: FlattenAction) {
        val target = action.target ?: return
        fakePlayer.nms.doBlockBreakAction(target, ABORT)
        action.target = null
        action.progress = 0f
    }

    override fun onStop(fakePlayer: FakePlayer, action: FlattenAction) {
        resetMining(fakePlayer, action)
    }

}
