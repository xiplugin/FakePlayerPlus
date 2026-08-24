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
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
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

        // 防飛天安全保護：若假人異常超出選區上限 + 2 格，強制校正回選區頂部
        if (player.location.y > action.maxY + 2) {
            val resetLoc = Location(player.world, player.location.x, (action.maxY + 1).toDouble(), player.location.z, player.location.yaw, player.location.pitch)
            player.teleport(resetLoc)
            val zeroVec = Vector(0.0, 0.0, 0.0)
            fakePlayer.nms.setDeltaMovement(zeroVec)
            player.velocity = zeroVec
        }

        // 初始化總需挖掘方塊計數
        if (action.totalBlocks == 0) {
            action.totalBlocks = countSolidBlocks(world, action)
        }

        // 自動拾取周圍掉落物
        if (action.pickupItems) {
            pickupNearbyItems(player)
        }

        // 背包滿載自動前往箱子清單依序存放物品
        val hasChests = action.chestLocations.isNotEmpty() || action.chestX != null
        if (action.autoDeposit && hasChests) {
            if (action.isDepositing) {
                val done = depositItemsToChest(fakePlayer, action)
                if (done) {
                    action.isDepositing = false
                }
                return
            } else if (isInventoryFull(player)) {
                action.isDepositing = true
                resetMining(fakePlayer, action)
                depositItemsToChest(fakePlayer, action)
                return
            }
        }

        // 尋找或驗證目標方塊（實體方塊或需填埋的液體方塊）
        val fillSlot = findFillBlockSlot(player)
        val canFillLiquid = fillSlot != null
        var target = action.target
        val isTargetValid = target != null && target.world == world && (isMinedBlock(target) || (canFillLiquid && isLiquidBlock(target)))
        if (!isTargetValid) {
            target = findNextBlock(world, action, player.location, canFillLiquid)
            if (target == null) {
                // 選區內方塊已全數清空，最後自動存放一次
                if (action.autoDeposit && hasChests) {
                    depositItemsToChest(fakePlayer, action)
                }
                action.clearedBlocks = action.totalBlocks
                com.coderxi.plugin.fakeplayer.repository.FlattenRepository().deleteTask(fakePlayer.uuid)
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
            if (isMinedBlock(target)) {
                com.coderxi.plugin.fakeplayer.utils.ToolHelper.equipBestTool(player, target)
            }
        }

        val targetCenter = target.location.add(0.5, 0.5, 0.5)
        val eyeLoc = player.eyeLocation
        val distToTarget = player.location.distance(targetCenter)

        // 若超出觸及距離，向目標方塊周圍的地面站立點行走靠近
        if (distToTarget > 3.5) {
            val standLoc = findStandLocation(player, target)
            val heightDiff = standLoc.y - player.location.y
            val isHighObstacle = heightDiff > 1.2 // 目標位於正常跳躍無法爬上的高台或懸崖

            if (isHighObstacle) {
                // 目標在無法爬上的高處懸崖/山頂，直接安全傳送至目標站立點
                val groundBlock = standLoc.block.getRelative(0, -1, 0)
                if (groundBlock.type.isSolid || groundBlock.isLiquid) {
                    player.teleport(standLoc)
                    val zeroVec = Vector(0.0, 0.0, 0.0)
                    fakePlayer.nms.setDeltaMovement(zeroVec)
                    player.velocity = zeroVec
                }
            } else {
                val canWalk = walkTowards(fakePlayer, standLoc)
                val lastLoc = action.lastLoc
                val isStagnant = lastLoc != null && lastLoc.distanceSquared(player.location) < 0.04

                if (isStagnant || !canWalk) {
                    action.stuckTick++
                    val threshold = if (!canWalk) 10 else 20
                    if (action.stuckTick >= threshold) {
                        val groundBlock = standLoc.block.getRelative(0, -1, 0)
                        if (standLoc != player.location && (groundBlock.type.isSolid || groundBlock.isLiquid)) {
                            player.teleport(standLoc)
                            val zeroVec = Vector(0.0, 0.0, 0.0)
                            fakePlayer.nms.setDeltaMovement(zeroVec)
                            player.velocity = zeroVec
                        }
                        action.stuckTick = 0
                    }
                } else {
                    action.lastLoc = player.location.clone()
                    action.stuckTick = 0
                }
            }

            if (player.location.distance(targetCenter) > 4.2) {
                return
            }
        } else {
            // 已在挖掘/填埋範圍內，立即煞車停止移動
            val stopVec = Vector(0.0, minOf(player.velocity.y, 0.0), 0.0)
            fakePlayer.nms.setDeltaMovement(stopVec)
            player.velocity = stopVec
        }

        // 調整假人視角朝向目標方塊
        val dir = targetCenter.toVector().subtract(eyeLoc.toVector())
        if (dir.lengthSquared() > 0.0001) {
            val yaw = Math.toDegrees(atan2(-dir.x, dir.z)).toFloat()
            val pitch = Math.toDegrees(atan2(-dir.y, sqrt(dir.x * dir.x + dir.z * dir.z))).toFloat()
            player.setRotation(yaw, pitch)
        }

        // 若目標為液體方塊，執行填埋覆蓋
        if (isLiquidBlock(target)) {
            val currentFillSlot = findFillBlockSlot(player)
            if (currentFillSlot != null) {
                val fillItem = player.inventory.getItem(currentFillSlot)
                if (fillItem != null && fillItem.type.isBlock && fillItem.type.isSolid) {
                    val blockType = fillItem.type
                    if (fillItem.amount > 1) {
                        fillItem.amount--
                    } else {
                        player.inventory.setItem(currentFillSlot, null)
                    }
                    player.swingMainHand()
                    target.type = blockType
                    world.playSound(target.location, org.bukkit.Sound.BLOCK_STONE_PLACE, 0.8f, 1.0f)
                    action.target = null
                    action.freezeTick = 3
                    return
                }
            }
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
            if (action.clearedBlocks % 20 == 0) {
                com.coderxi.plugin.fakeplayer.repository.FlattenRepository().updateTaskProgress(fakePlayer.uuid, action.totalBlocks, action.clearedBlocks)
            }
            fakePlayer.nms.doBlockBreakAction(target, STOP)
            if (isMinedBlock(target)) {
                player.breakBlock(target)
            }
            resetMining(fakePlayer, action)
            action.freezeTick = action.tickDelay
            if (action.pickupItems) {
                pickupNearbyItems(player)
            }
        }
    }

    private fun isInventoryFull(player: Player): Boolean {
        val inv = player.inventory
        var emptySlots = 0
        for (slot in 0..35) {
            val item = inv.getItem(slot)
            if (item == null || item.type.isAir) {
                emptySlots++
            }
        }
        return emptySlots <= 2 && hasItemsToDeposit(player)
    }

    private fun hasItemsToDeposit(player: Player): Boolean {
        val playerInv = player.inventory
        for (slot in 0..35) {
            val item = playerInv.getItem(slot) ?: continue
            if (item.type.isAir) continue
            val name = item.type.name
            val isTool = name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE") ||
                    name.endsWith("_HOE") || name.endsWith("_SWORD") || name == "SHEARS" ||
                    name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
            if (!isTool) return true
        }
        return false
    }

    private fun depositItemsToChest(fakePlayer: FakePlayer, action: FlattenAction): Boolean {
        val player = fakePlayer.player
        val chestList = mutableListOf<Location>()
        if (action.chestLocations.isNotEmpty()) {
            chestList.addAll(action.chestLocations)
        } else if (action.chestX != null && action.chestY != null && action.chestZ != null) {
            val w = action.chestWorld ?: player.world
            chestList.add(Location(w, action.chestX!!.toDouble(), action.chestY!!.toDouble(), action.chestZ!!.toDouble()))
        }

        if (chestList.isEmpty()) return true

        var depositedAny = false

        for (loc in chestList) {
            val world = loc.world ?: action.world ?: player.world
            val chestBlock = world.getBlockAt(loc)
            val blockState = chestBlock.getState(false)
            if (blockState !is Container) continue

            val targetInv = if (blockState is Chest) {
                val holder = blockState.inventory.holder
                if (holder is DoubleChest) {
                    holder.inventory
                } else {
                    blockState.inventory
                }
            } else {
                blockState.inventory
            }

            val chestCenter = chestBlock.location.clone().add(0.5, 0.5, 0.5)
            val dist = player.location.distance(chestCenter)

            if (dist > 3.5) {
                val standLoc = findStandLocation(player, chestBlock)
                val canWalk = walkTowards(fakePlayer, standLoc)
                if (!canWalk || dist > 6.0 || player.location.distance(chestCenter) > 4.2) {
                    player.teleport(standLoc)
                }
                if (player.location.distance(chestCenter) > 4.2) {
                    return false
                }
            }

            // 朝向箱子並揮手打開存放
            val eyeLoc = player.eyeLocation
            val dir = chestCenter.toVector().subtract(eyeLoc.toVector())
            if (dir.lengthSquared() > 0.0001) {
                val yaw = Math.toDegrees(atan2(-dir.x, dir.z)).toFloat()
                val pitch = Math.toDegrees(atan2(-dir.y, sqrt(dir.x * dir.x + dir.z * dir.z))).toFloat()
                player.setRotation(yaw, pitch)
            }
            player.swingMainHand()
            world.playSound(chestBlock.location, org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f)

            val playerInv = player.inventory

            for (slot in 0..35) {
                val item = playerInv.getItem(slot) ?: continue
                if (item.type.isAir) continue
                val name = item.type.name

                val isTool = name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE") ||
                        name.endsWith("_HOE") || name.endsWith("_SWORD") || name == "SHEARS" ||
                        name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")

                if (!isTool) {
                    val remaining = targetInv.addItem(item)
                    if (remaining.isEmpty()) {
                        playerInv.setItem(slot, null)
                        depositedAny = true
                    } else {
                        playerInv.setItem(slot, remaining.values.first())
                        depositedAny = true
                    }
                }
            }

            // 若身上物資已全數存入，跳出箱子循環
            if (!hasItemsToDeposit(player)) {
                break
            }
        }

        if (hasItemsToDeposit(player)) {
            fakePlayer.owners.forEach {
                it.sendMessage(tlp("fakeplayer.flatten.chest.full", fakePlayer.name))
            }
        } else if (depositedAny) {
            fakePlayer.owners.forEach {
                it.sendMessage(tlp("fakeplayer.flatten.chest.deposited", fakePlayer.name))
            }
        }

        return true
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

    private fun walkTowards(fakePlayer: FakePlayer, targetLoc: Location): Boolean {
        val player = fakePlayer.player
        val pLoc = player.location
        val dx = targetLoc.x - pLoc.x
        val dy = targetLoc.y - pLoc.y
        val dz = targetLoc.z - pLoc.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        if (horizontalDist < 0.25) return true

        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        player.setRotation(yaw, player.location.pitch)

        val inWater = player.isInWater || pLoc.block.isLiquid
        val speed = if (inWater) 0.16 else 0.22
        val vx = (dx / horizontalDist) * speed
        val vz = (dz / horizontalDist) * speed

        if (inWater) {
            fakePlayer.nms.setJumping(false)
            val vy = if (dy > 0.5 && pLoc.y < targetLoc.y) 0.05 else if (dy < -0.5) -0.08 else minOf(player.velocity.y, 0.0)
            val moveVec = Vector(vx, vy, vz)
            fakePlayer.nms.setDeltaMovement(moveVec)
            player.velocity = moveVec
            return true
        }

        val frontLoc = pLoc.clone().add((dx / horizontalDist) * 0.6, 0.0, (dz / horizontalDist) * 0.6)
        val frontBlock = frontLoc.block
        val frontAbove = frontLoc.clone().add(0.0, 1.0, 0.0).block

        // 懸崖深坑與岩漿邊緣探測防掉落
        val drop1 = frontLoc.clone().add(0.0, -1.0, 0.0).block
        val drop2 = frontLoc.clone().add(0.0, -2.0, 0.0).block
        val drop3 = frontLoc.clone().add(0.0, -3.0, 0.0).block

        val isLavaHazard = drop1.type == org.bukkit.Material.LAVA || drop2.type == org.bukkit.Material.LAVA
        val isDeepDrop = drop1.type.isAir && drop2.type.isAir && drop3.type.isAir
        val isCliffHazard = (isDeepDrop || isLavaHazard) && (targetLoc.y >= pLoc.y - 1.0)

        if (isCliffHazard && fakePlayer.nms.onGround) {
            // 前方為深坑/懸崖/岩漿，立即停步煞車防掉落
            val stopVec = Vector(0.0, minOf(player.velocity.y, 0.0), 0.0)
            fakePlayer.nms.setDeltaMovement(stopVec)
            player.velocity = stopVec
            return false
        }

        val obstacleInFront = !frontBlock.type.isAir && frontBlock.type.isSolid && frontAbove.type.isAir

        if (obstacleInFront && fakePlayer.nms.onGround) {
            fakePlayer.nms.jumpFromGround()
            fakePlayer.nms.setJumping(true)
            val moveVec = Vector(vx, 0.42, vz)
            fakePlayer.nms.setDeltaMovement(moveVec)
            player.velocity = moveVec
        } else {
            fakePlayer.nms.setJumping(false)
            // 陸地行走保持正常重力下降，絕不覆蓋正向向上速度
            val currentVy = minOf(player.velocity.y, 0.0)
            val moveVec = Vector(vx, currentVy, vz)
            fakePlayer.nms.setDeltaMovement(moveVec)
            player.velocity = moveVec
        }
        return true
    }

    private fun findStandLocation(player: Player, target: Block): Location {
        val world = target.world
        val pLoc = player.location
        val candidates = mutableListOf<Location>()
        val offsets = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(-1, 1),
            intArrayOf(2, 0), intArrayOf(-2, 0), intArrayOf(0, 2), intArrayOf(0, -2),
            intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(-2, 1), intArrayOf(-2, -1),
            intArrayOf(1, 2), intArrayOf(-1, 2), intArrayOf(1, -2), intArrayOf(-1, -2)
        )
        for (offset in offsets) {
            val sx = target.x + offset[0]
            val sz = target.z + offset[1]
            val startY = minOf(target.y + 3, world.maxHeight - 2)
            val minY = maxOf(target.y - 4, world.minHeight)
            for (sy in startY downTo minY) {
                val ground = world.getBlockAt(sx, sy, sz)
                val feet = world.getBlockAt(sx, sy + 1, sz)
                val head = world.getBlockAt(sx, sy + 2, sz)
                val groundOk = !ground.type.isAir && (ground.type.isSolid || ground.isLiquid)
                val spaceOk = (feet.type.isAir || feet.isLiquid) && (head.type.isAir || head.isLiquid)
                if (groundOk && spaceOk) {
                    candidates.add(Location(world, sx + 0.5, (sy + 1).toDouble(), sz + 0.5))
                    break
                }
            }
        }
        val targetCenter = target.location.clone().add(0.5, 0.5, 0.5)
        val inReach = candidates.filter { it.distanceSquared(targetCenter) <= 12.25 }
        if (inReach.isNotEmpty()) {
            val topLevel = inReach.filter { it.y >= target.y }
            if (topLevel.isNotEmpty()) {
                return topLevel.minByOrNull { it.distanceSquared(pLoc) }!!
            }
            return inReach.minByOrNull { it.distanceSquared(pLoc) }!!
        }
        return candidates.minByOrNull { it.distanceSquared(pLoc) } ?: pLoc
    }

    private fun findFillBlockSlot(player: Player): Int? {
        val inv = player.inventory
        for (slot in 0..35) {
            val item = inv.getItem(slot) ?: continue
            if (item.type.isAir || item.amount <= 0) continue
            val type = item.type
            if (type.isBlock && type.isSolid && !isExcludedFillBlock(type)) {
                return slot
            }
        }
        return null
    }

    private fun isExcludedFillBlock(material: org.bukkit.Material): Boolean {
        val name = material.name
        return name.contains("ORE") || name.contains("CHEST") || name.contains("SHULKER") ||
                name.contains("COMMAND") || name.contains("BED") || name.contains("DOOR") ||
                name.contains("FURNACE") || name.contains("ANVIL") || name.contains("HOPPER") ||
                name.contains("SPAWNER") || name.contains("BEACON") || name.contains("ENCHANTING")
    }

    private fun isLiquidBlock(block: Block): Boolean {
        val type = block.type
        return block.isLiquid || type == org.bukkit.Material.WATER || type == org.bukkit.Material.LAVA || type == org.bukkit.Material.BUBBLE_COLUMN
    }

    private fun isMinedBlock(block: Block): Boolean {
        val type = block.type
        if (type.isAir) return false
        if (type == org.bukkit.Material.SEAGRASS || type == org.bukkit.Material.TALL_SEAGRASS ||
            type == org.bukkit.Material.KELP || type == org.bukkit.Material.KELP_PLANT ||
            type == org.bukkit.Material.LILY_PAD) {
            return true
        }
        if (isLiquidBlock(block)) {
            return false
        }
        if (type.hardness < 0f) return false
        return true
    }

    private fun findNextBlock(world: org.bukkit.World, action: FlattenAction, currentLoc: Location, canFillLiquid: Boolean = false): Block? {
        val playerBlockX = currentLoc.blockX
        val playerBlockZ = currentLoc.blockZ
        val playerBlockY = currentLoc.blockY

        for (y in action.maxY downTo action.minY) {
            var closestBlock: Block? = null
            var minScore = Double.MAX_VALUE
            for (x in action.minX..action.maxX) {
                for (z in action.minZ..action.maxZ) {
                    val block = world.getBlockAt(x, y, z)
                    val isMinable = isMinedBlock(block) && (!action.preserveOres || !isOreBlock(block))
                    val isLiquid = canFillLiquid && isLiquidBlock(block)
                    if (isMinable || isLiquid) {
                        var score = block.location.distanceSquared(currentLoc)
                        // 若為玩家正腳下的方塊，給予優先級懲罰，優先挖掘周圍方塊以防止挖坑掉落受困
                        if (x == playerBlockX && z == playerBlockZ && y <= playerBlockY) {
                            score += 100.0
                        }
                        if (score < minScore) {
                            minScore = score
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
                    if (isMinedBlock(b)) {
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
