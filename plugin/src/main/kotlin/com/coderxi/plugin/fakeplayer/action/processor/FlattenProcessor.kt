package com.coderxi.plugin.fakeplayer.action.processor

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.action.ActionTrack
import com.coderxi.plugin.fakeplayer.api.action.FlattenAction
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer.BlockBreakActionType.*
import com.coderxi.plugin.fakeplayer.utils.tlp
import org.bukkit.Location
import org.bukkit.attribute.Attribute
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

        // 提高假人跨步高度，使其能平滑走上一格高的台階與坑洞邊緣
        player.getAttribute(Attribute.STEP_HEIGHT)?.let {
            if (it.baseValue < 1.25) it.baseValue = 1.25
        }

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
            com.coderxi.plugin.fakeplayer.utils.ToolHelper.equipBestTool(player, target)
        }

        val targetCenter = target.location.add(0.5, 0.5, 0.5)
        val eyeLoc = player.eyeLocation
        val distToTarget = player.location.distance(targetCenter)

        // 若超出觸及距離，向目標方塊周圍的地面站立點行走靠近
        if (distToTarget > 3.5) {
            val standLoc = findStandLocation(player, target)
            walkTowards(fakePlayer, standLoc)

            // 卡住檢測與地面自動脫困
            val lastLoc = action.lastLoc
            if (lastLoc != null && lastLoc.distanceSquared(player.location) < 0.04) {
                action.stuckTick++
                if (action.stuckTick >= 40) { // 在坑內或障礙卡住超過 2 秒
                    val groundBlock = standLoc.block.getRelative(0, -1, 0)
                    if (standLoc != player.location && groundBlock.type.isSolid) {
                        player.teleport(standLoc)
                    }
                    action.stuckTick = 0
                }
            } else {
                action.lastLoc = player.location.clone()
                action.stuckTick = 0
            }

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
        val dy = targetLoc.y - pLoc.y
        val dz = targetLoc.z - pLoc.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        if (horizontalDist < 0.25) return

        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        player.setRotation(yaw, player.location.pitch)

        val inWater = player.isInWater || pLoc.block.isLiquid
        val speed = if (inWater) 0.16 else 0.22
        val vx = (dx / horizontalDist) * speed
        val vz = (dz / horizontalDist) * speed

        if (inWater) {
            fakePlayer.nms.setJumping(false)
            val vy = if (dy > 0.3) 0.08 else if (dy < -0.3) -0.10 else 0.0
            val moveVec = Vector(vx, vy, vz)
            fakePlayer.nms.setDeltaMovement(moveVec)
            player.velocity = moveVec
        } else {
            val frontLoc = pLoc.clone().add((dx / horizontalDist) * 0.6, 0.0, (dz / horizontalDist) * 0.6)
            val frontBlock = frontLoc.block
            val frontAbove = frontLoc.clone().add(0.0, 1.0, 0.0).block
            val obstacleInFront = !frontBlock.type.isAir && frontBlock.type.isSolid && frontAbove.type.isAir

            if (obstacleInFront && fakePlayer.nms.onGround) {
                fakePlayer.nms.jumpFromGround()
                fakePlayer.nms.setJumping(true)
                val moveVec = Vector(vx, 0.42, vz)
                fakePlayer.nms.setDeltaMovement(moveVec)
                player.velocity = moveVec
            } else {
                fakePlayer.nms.setJumping(false)
                // 陸地行走保持正常重力與水平速度，不強行施加持續向上動量
                val currentVy = minOf(player.velocity.y, 0.1)
                val moveVec = Vector(vx, currentVy, vz)
                fakePlayer.nms.setDeltaMovement(moveVec)
                player.velocity = moveVec
            }
        }
    }

    private fun findStandLocation(player: Player, target: Block): Location {
        val world = target.world
        val pLoc = player.location
        val candidates = mutableListOf<Location>()
        val offsets = arrayOf(
            intArrayOf(0, 0),
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(-1, 1),
            intArrayOf(2, 0), intArrayOf(-2, 0), intArrayOf(0, 2), intArrayOf(0, -2)
        )
        for (offset in offsets) {
            val sx = target.x + offset[0]
            val sz = target.z + offset[1]
            val startY = minOf(target.y + 1, world.maxHeight - 2)
            val minY = maxOf(target.y - 12, world.minHeight)
            for (sy in startY downTo minY) {
                val ground = world.getBlockAt(sx, sy, sz)
                val feet = world.getBlockAt(sx, sy + 1, sz)
                val head = world.getBlockAt(sx, sy + 2, sz)
                if (!ground.type.isAir && ground.type.isSolid && feet.type.isAir && head.type.isAir) {
                    candidates.add(Location(world, sx + 0.5, (sy + 1).toDouble(), sz + 0.5))
                    break
                }
            }
        }
        // 嚴格確保只回傳地面有效站立點，若周圍無地面則保持當前玩家位置，絕不回傳半空中
        return candidates.minByOrNull { it.distanceSquared(pLoc) } ?: pLoc
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
        fakePlayer.nms.setJumping(false)
        fakePlayer.player.getAttribute(Attribute.STEP_HEIGHT)?.let {
            it.baseValue = 0.6
        }
    }

}
