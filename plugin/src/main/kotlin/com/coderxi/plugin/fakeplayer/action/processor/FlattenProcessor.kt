package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionTrack
import com.coderxi.plugin.fakeplayer.api.action.FlattenAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer.BlockBreakActionType.*
import com.coderxi.plugin.fakeplayer.utils.tlp
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.atan2
import kotlin.math.sqrt

object FlattenProcessor : ActionProcessor<FlattenAction> {

    override val actionType get() = FlattenAction::class.java

    override fun process(fakePlayer: FakePlayer, action: FlattenAction, handler: ActionHandler) {
        if (action.freezeTick > 0) { action.freezeTick--; return }
        val player = fakePlayer.player
        val world = action.world ?: player.world

        // 初始化總需挖掘方塊計數
        if (action.totalBlocks == 0) {
            action.totalBlocks = countSolidBlocks(world, action)
        }

        // 自動拾取周圍掉落物
        if (action.pickupItems) {
            pickupNearbyItems(player)
        }

        // 尋找或驗證目標方塊
        var target = action.target
        if (target == null || target.type.isAir || target.world != world) {
            target = findNextBlock(world, action, player.location)
            if (target == null) {
                // 選區內方塊已全數清空
                action.clearedBlocks = action.totalBlocks
                resetMining(fakePlayer, action)
                fakePlayer.owners.forEach {
                    it.sendMessage(tlp("fakeplayer.flatten.complete", fakePlayer.name))
                }
                handler.stop(ActionTrack.INTERACTION)
                return
            }
            action.target = target
            action.lastTargetName = target.type.name
            action.progress = 0f
        }

        val targetCenter = target.location.add(0.5, 0.5, 0.5)
        val eyeLoc = player.eyeLocation
        val distToTarget = player.location.distance(targetCenter)

        // 若超出觸及距離，向目標方塊行走靠近
        if (distToTarget > 3.5) {
            val standLoc = findStandLocation(player, target)
            walkTowards(fakePlayer, standLoc)
            if (player.location.distance(targetCenter) > 4.2) {
                return
            }
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
            action.clearedBlocks++
            fakePlayer.nms.doBlockBreakAction(target, STOP)
            if (!target.type.isAir) {
                player.breakBlock(target)
            }
            resetMining(fakePlayer, action)
            action.freezeTick = action.tickDelay
            if (action.pickupItems) {
                pickupNearbyItems(player)
            }
        }
    }

    private fun pickupNearbyItems(player: Player) {
        val nearbyItems = player.getNearbyEntities(2.5, 2.5, 2.5).filterIsInstance<Item>()
        for (itemEntity in nearbyItems) {
            if (!itemEntity.isValid || itemEntity.isDead) continue
            val remaining = player.inventory.addItem(itemEntity.itemStack)
            if (remaining.isEmpty()) {
                itemEntity.remove()
            } else {
                itemEntity.itemStack = remaining.values.first()
            }
        }
    }

    private fun walkTowards(fakePlayer: FakePlayer, targetLoc: Location) {
        val player = fakePlayer.player
        val pLoc = player.location
        val dx = targetLoc.x - pLoc.x
        val dz = targetLoc.z - pLoc.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        if (horizontalDist < 0.3) return

        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        player.setRotation(yaw, player.location.pitch)

        val speed = 0.22 // 原版正常行走速度
        val vx = (dx / horizontalDist) * speed
        val vz = (dz / horizontalDist) * speed

        val frontLoc = pLoc.clone().add((dx / horizontalDist) * 0.6, 0.0, (dz / horizontalDist) * 0.6)
        val frontBlock = frontLoc.block
        val frontAbove = frontLoc.clone().add(0.0, 1.0, 0.0).block
        val needJump = (!frontBlock.type.isAir && frontBlock.type.isSolid && frontAbove.type.isAir && fakePlayer.nms.onGround)

        var vy = player.velocity.y
        if (needJump) {
            vy = 0.42
        }

        val moveVec = Vector(vx, vy, vz)
        fakePlayer.nms.setDeltaMovement(moveVec)
        player.velocity = moveVec
    }

    private fun findStandLocation(player: Player, target: Block): Location {
        val world = target.world
        val pLoc = player.location
        val candidates = mutableListOf<Location>()
        val offsets = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(-1, 1),
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
        return candidates.minByOrNull { it.distanceSquared(pLoc) }
            ?: Location(world, target.x + 0.5, (target.y + 1).toDouble(), target.z + 0.5)
    }

    private fun findNextBlock(world: org.bukkit.World, action: FlattenAction, currentLoc: Location): Block? {
        for (y in action.maxY downTo action.minY) {
            var closestBlock: Block? = null
            var minDistanceSq = Double.MAX_VALUE
            for (x in action.minX..action.maxX) {
                for (z in action.minZ..action.maxZ) {
                    val block = world.getBlockAt(x, y, z)
                    if (!block.type.isAir && block.type.hardness >= 0f) {
                        if (action.preserveOres && isOreBlock(block)) {
                            continue
                        }
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

    private fun isOreBlock(block: Block): Boolean {
        val name = block.type.name
        return name.endsWith("_ORE") || name.contains("ORE") || name == "ANCIENT_DEBRIS"
    }

    private fun countSolidBlocks(world: org.bukkit.World, action: FlattenAction): Int {
        var count = 0
        for (y in action.minY..action.maxY) {
            for (x in action.minX..action.maxX) {
                for (z in action.minZ..action.maxZ) {
                    val b = world.getBlockAt(x, y, z)
                    if (!b.type.isAir && b.type.hardness >= 0f) {
                        if (!action.preserveOres || !isOreBlock(b)) {
                            count++
                        }
                    }
                }
            }
        }
        return count
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
