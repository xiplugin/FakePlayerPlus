package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionTrack
import com.coderxi.plugin.fakeplayer.api.action.FlattenAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer.BlockBreakActionType.*
import com.coderxi.plugin.fakeplayer.utils.tlp
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import kotlin.math.atan2
import kotlin.math.sqrt

object FlattenProcessor : ActionProcessor<FlattenAction> {

    override val actionType get() = FlattenAction::class.java

    override fun process(fakePlayer: FakePlayer, action: FlattenAction, handler: ActionHandler) {
        if (action.freezeTick > 0) { action.freezeTick--; return }
        val player = fakePlayer.player
        val world = action.world ?: player.world

        // 尋找或驗證目標方塊
        var target = action.target
        if (target == null || target.type.isAir || target.world != world) {
            target = findNextBlock(world, action, player.location)
            if (target == null) {
                // 選區內方塊已全數清空
                resetMining(fakePlayer, action)
                fakePlayer.owners.forEach {
                    it.sendMessage(tlp("fakeplayer.flatten.complete", fakePlayer.name))
                }
                handler.stop(ActionTrack.INTERACTION)
                return
            }
            action.target = target
            action.progress = 0f
            ensureReach(player, target)
        }

        // 確保假人在挖掘距離內
        val targetCenter = target.location.add(0.5, 0.5, 0.5)
        val eyeLoc = player.eyeLocation
        if (player.location.distance(targetCenter) > 3.8) {
            ensureReach(player, target)
        }

        // 調整假人視角朝向目標方塊
        val dir = targetCenter.toVector().subtract(eyeLoc.toVector())
        if (dir.lengthSquared() > 0.0001) {
            val yaw = Math.toDegrees(atan2(-dir.x, dir.z)).toFloat()
            val pitch = Math.toDegrees(atan2(-dir.y, sqrt(dir.x * dir.x + dir.z * dir.z))).toFloat()
            player.setRotation(yaw, pitch)
        }

        player.swingMainHand()

        // 計算破壞進度
        val rawProgress = fakePlayer.nms.getDestroyProgress(target)
        val step = if (rawProgress > 0f) rawProgress else 0.05f

        if (action.progress == 0f) {
            fakePlayer.nms.doBlockBreakAction(target, START)
        }

        action.progress += step

        if (action.progress >= 1.0f) {
            fakePlayer.nms.doBlockBreakAction(target, STOP)
            // 使用 Paper API 確保方塊被破壞並觸發掉落
            if (!target.type.isAir) {
                player.breakBlock(target)
            }
            resetMining(fakePlayer, action)
            action.freezeTick = 2
        }
    }

    private fun ensureReach(player: Player, target: Block) {
        val targetLoc = target.location.add(0.5, 0.0, 0.5)
        val pLoc = player.location
        if (pLoc.distance(targetLoc) <= 3.8) return

        val world = target.world
        val candidates = mutableListOf<Location>()
        val offsets = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(2, 0), intArrayOf(-2, 0), intArrayOf(0, 2), intArrayOf(0, -2)
        )
        for (offset in offsets) {
            val sx = target.x + offset[0]
            val sz = target.z + offset[1]
            val sy = target.y
            for (dy in 1 downTo -2) {
                val ground = world.getBlockAt(sx, sy + dy, sz)
                val feet = world.getBlockAt(sx, sy + dy + 1, sz)
                val head = world.getBlockAt(sx, sy + dy + 2, sz)
                if (!ground.type.isAir && feet.type.isAir && head.type.isAir) {
                    candidates.add(Location(world, sx + 0.5, (sy + dy + 1).toDouble(), sz + 0.5))
                    break
                }
            }
        }
        val best = candidates.minByOrNull { it.distanceSquared(pLoc) }
            ?: Location(world, target.x + 0.5, (target.y + 1).toDouble(), target.z + 0.5)
        player.teleport(best)
    }

    private fun findNextBlock(world: org.bukkit.World, action: FlattenAction, currentLoc: Location): Block? {
        // 由上往下（maxY -> minY）優先搜尋，同層內優先選擇距離假人最近的方塊
        for (y in action.maxY downTo action.minY) {
            var closestBlock: Block? = null
            var minDistanceSq = Double.MAX_VALUE
            for (x in action.minX..action.maxX) {
                for (z in action.minZ..action.maxZ) {
                    val block = world.getBlockAt(x, y, z)
                    if (!block.type.isAir && block.type.hardness >= 0f) {
                        val dSq = block.location.distanceSquared(currentLoc)
                        if (dSq < minDistanceSq) {
                            minDistanceSq = dSq
                            closestBlock = block
                        }
                    }
                }
            }
            if (closestBlock != null) {
                return closestBlock
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
