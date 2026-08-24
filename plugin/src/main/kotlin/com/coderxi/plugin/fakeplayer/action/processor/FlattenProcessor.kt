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

        // 尋找或驗證目標方塊（實體可挖掘方塊或待抽乾液體）
        var target = action.target
        val isTargetValid = target != null && target.world == world && (isMinedBlock(target) || isLiquidBlock(target)) && (!action.preserveOres || !isOreBlock(target))
        if (!isTargetValid) {
            target = findNextBlock(world, action, player.location, fakePlayer.uuid)
            if (target == null) {
                // 選區內方塊已全數清空，最後自動存放一次
                if (action.autoDeposit && hasChests) {
                    depositItemsToChest(fakePlayer, action)
                }
                action.clearedBlocks = action.totalBlocks
                com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.release(fakePlayer.uuid)
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
            com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.reserve(world, target.x, target.y, target.z, fakePlayer.uuid)
            if (isMinedBlock(target)) {
                if (hasChests && !com.coderxi.plugin.fakeplayer.utils.ToolHelper.hasToolForBlock(player, target)) {
                    restockToolsFromBoundChests(fakePlayer, action, com.coderxi.plugin.fakeplayer.utils.ToolHelper.getNeededToolSuffix(target))
                }
                com.coderxi.plugin.fakeplayer.utils.ToolHelper.equipBestTool(player, target)
            }
        }

        val targetCenter = target.location.add(0.5, 0.5, 0.5)
        val eyeLoc = player.eyeLocation
        val distToTarget = player.location.distance(targetCenter)

        // 若超出觸及距離，向目標方塊周圍的地面站立點行走靠近
        if (distToTarget > 3.5) {
            val standLoc = findStandLocation(player, target)
            val canWalk = walkTowards(fakePlayer, standLoc)
            val lastLoc = action.lastLoc
            val isStagnant = lastLoc != null && lastLoc.distanceSquared(player.location) < 0.02

            if (isStagnant || !canWalk) {
                action.stuckTick++
                // 給予充分時間進行搭橋與墊腳（60 ticks = 3 秒），絕不輕易瞬移
                val threshold = if (!canWalk) 40 else 60
                if (action.stuckTick >= threshold) {
                    val groundBlock = standLoc.block.getRelative(0, -1, 0)
                    if (standLoc != player.location && (groundBlock.type.isSolid || groundBlock.isLiquid)) {
                        // 僅在極端卡死且完全無法搭路時作為最後一道防護傳送
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

        // 若目標為水/岩漿液體方塊，直接進行抽乾/清理，絕不放置實體方塊造成反覆挖掘死循環
        if (isLiquidBlock(target)) {
            player.swingMainHand()
            val isLava = target.type == org.bukkit.Material.LAVA
            val data = target.blockData
            if (data is org.bukkit.block.data.Waterlogged && data.isWaterlogged) {
                data.isWaterlogged = false
                target.blockData = data
            } else {
                target.setType(org.bukkit.Material.AIR, false)
            }
            val sound = if (isLava) org.bukkit.Sound.ITEM_BUCKET_FILL_LAVA else org.bukkit.Sound.ITEM_BUCKET_FILL
            world.playSound(target.location, sound, 0.8f, 1.0f)
            action.clearedBlocks++
            if (action.clearedBlocks % 20 == 0) {
                com.coderxi.plugin.fakeplayer.repository.FlattenRepository().updateTaskProgress(fakePlayer.uuid, action.totalBlocks, action.clearedBlocks)
            }
            com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.release(fakePlayer.uuid)
            action.target = null
            action.freezeTick = 2
            return
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
        if (action.outputChestLocations.isNotEmpty()) {
            chestList.addAll(action.outputChestLocations)
        } else if (action.chestLocations.isNotEmpty()) {
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

            // 存完物品後，若未單獨設定工具補給箱，且身上缺少核心工具，自動從此處補給工具
            if (action.toolChestLocations.isEmpty() && !com.coderxi.plugin.fakeplayer.utils.ToolHelper.hasAllEssentialTools(player)) {
                val restocked = com.coderxi.plugin.fakeplayer.utils.ToolHelper.restockToolsFromChest(player, targetInv)
                if (restocked) {
                    fakePlayer.owners.forEach {
                        it.sendMessage(tlp("fakeplayer.flatten.chest.restocked", fakePlayer.name))
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

    private fun restockToolsFromBoundChests(fakePlayer: FakePlayer, action: FlattenAction, neededSuffix: String? = null): Boolean {
        val player = fakePlayer.player
        val chestList = mutableListOf<Location>()
        if (action.toolChestLocations.isNotEmpty()) {
            chestList.addAll(action.toolChestLocations)
        } else if (action.chestLocations.isNotEmpty()) {
            chestList.addAll(action.chestLocations)
        } else if (action.outputChestLocations.isNotEmpty()) {
            chestList.addAll(action.outputChestLocations)
        } else if (action.chestX != null && action.chestY != null && action.chestZ != null) {
            val w = action.chestWorld ?: player.world
            chestList.add(Location(w, action.chestX!!.toDouble(), action.chestY!!.toDouble(), action.chestZ!!.toDouble()))
        }

        if (chestList.isEmpty()) return false

        for (loc in chestList) {
            val world = loc.world ?: action.world ?: player.world
            val chestBlock = world.getBlockAt(loc)
            val blockState = chestBlock.getState(false)
            if (blockState !is Container) continue

            val targetInv = if (blockState is Chest) {
                val holder = blockState.inventory.holder
                if (holder is DoubleChest) holder.inventory else blockState.inventory
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
            }

            // 朝向箱子並揮手打開
            val eyeLoc = player.eyeLocation
            val dir = chestCenter.toVector().subtract(eyeLoc.toVector())
            if (dir.lengthSquared() > 0.0001) {
                val yaw = Math.toDegrees(atan2(-dir.x, dir.z)).toFloat()
                val pitch = Math.toDegrees(atan2(-dir.y, sqrt(dir.x * dir.x + dir.z * dir.z))).toFloat()
                player.setRotation(yaw, pitch)
            }
            player.swingMainHand()
            world.playSound(chestBlock.location, org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f)

            val restocked = com.coderxi.plugin.fakeplayer.utils.ToolHelper.restockToolsFromChest(player, targetInv, neededSuffix)
            if (restocked) {
                fakePlayer.owners.forEach {
                    it.sendMessage(tlp("fakeplayer.flatten.chest.restocked", fakePlayer.name))
                }
                return true
            }
        }
        return false
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

    private fun placeScaffoldBlock(fakePlayer: FakePlayer, targetBlock: Block): Boolean {
        val player = fakePlayer.player
        val slot = findFillBlockSlot(player) ?: return false
        val item = player.inventory.getItem(slot) ?: return false
        if (item.type.isAir || item.amount <= 0) return false

        val mat = item.type
        if (!mat.isBlock || !mat.isSolid || isExcludedFillBlock(mat)) return false

        if (item.amount > 1) {
            item.amount--
        } else {
            player.inventory.setItem(slot, null)
        }

        targetBlock.type = mat
        player.swingMainHand()
        targetBlock.world.playSound(targetBlock.location, org.bukkit.Sound.BLOCK_STONE_PLACE, 0.8f, 1.0f)
        return true
    }

    private fun walkTowards(fakePlayer: FakePlayer, targetLoc: Location): Boolean {
        val player = fakePlayer.player
        val pLoc = player.location
        val dx = targetLoc.x - pLoc.x
        val dy = targetLoc.y - pLoc.y
        val dz = targetLoc.z - pLoc.z
        val horizontalDist = sqrt(dx * dx + dz * dz)

        if (horizontalDist < 0.3 && kotlin.math.abs(dy) < 0.6) return true

        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        player.setRotation(yaw, player.location.pitch)

        val inWater = player.isInWater || pLoc.block.isLiquid
        val speed = if (inWater) 0.16 else 0.22
        val vx = if (horizontalDist > 0.001) (dx / horizontalDist) * speed else 0.0
        val vz = if (horizontalDist > 0.001) (dz / horizontalDist) * speed else 0.0

        if (inWater) {
            fakePlayer.nms.setJumping(false)
            val vy = if (dy > 0.5 && pLoc.y < targetLoc.y) 0.08 else if (dy < -0.5) -0.08 else minOf(player.velocity.y, 0.0)
            val moveVec = Vector(vx, vy, vz)
            fakePlayer.nms.setDeltaMovement(moveVec)
            player.velocity = moveVec
            return true
        }

        val stepDirX = if (horizontalDist > 0.001) dx / horizontalDist else 0.0
        val stepDirZ = if (horizontalDist > 0.001) dz / horizontalDist else 0.0
        val frontLoc = pLoc.clone().add(stepDirX * 0.7, 0.0, stepDirZ * 0.7)
        val frontBlock = frontLoc.block
        val frontAbove = frontLoc.clone().add(0.0, 1.0, 0.0).block
        val currentFeet = pLoc.block

        // 1. 懸空峽谷/深坑自動搭橋鋪路 (Auto-Bridging across gaps & ravines)
        val drop1 = frontLoc.clone().add(0.0, -1.0, 0.0).block
        val drop2 = frontLoc.clone().add(0.0, -2.0, 0.0).block
        val isDeepDrop = (drop1.type.isAir || drop1.isLiquid) && (drop2.type.isAir || drop2.isLiquid)

        if (isDeepDrop) {
            val bridgeBlock = if (dy >= -1.0) drop1 else drop2
            if (bridgeBlock.type.isAir || bridgeBlock.isLiquid) {
                val placed = placeScaffoldBlock(fakePlayer, bridgeBlock)
                if (placed) {
                    player.setRotation(yaw, 60.0f)
                } else if (fakePlayer.nms.onGround) {
                    // 無方塊可搭時停步煞車防掉落深淵
                    val stopVec = Vector(0.0, minOf(player.velocity.y, 0.0), 0.0)
                    fakePlayer.nms.setDeltaMovement(stopVec)
                    player.velocity = stopVec
                    return false
                }
            }
        }

        // 2. 攀爬高台/高於1格的斷崖自動墊腳搭路 (Pillaring / Scaffolding up cliffs)
        val isHighWall = (!frontBlock.type.isAir && frontBlock.type.isSolid && !frontAbove.type.isAir)
        val targetMuchHigher = dy > 1.0 && horizontalDist < 2.5

        if ((isHighWall || targetMuchHigher) && fakePlayer.nms.onGround) {
            val placed = placeScaffoldBlock(fakePlayer, currentFeet)
            if (placed) {
                fakePlayer.nms.jumpFromGround()
                fakePlayer.nms.setJumping(true)
                val moveVec = Vector(vx * 0.5, 0.52, vz * 0.5)
                fakePlayer.nms.setDeltaMovement(moveVec)
                player.velocity = moveVec
                return true
            }
        }

        // 3. 正常1格高障礙跳躍與行走
        val obstacleInFront = !frontBlock.type.isAir && frontBlock.type.isSolid && frontAbove.type.isAir

        if (obstacleInFront && fakePlayer.nms.onGround) {
            fakePlayer.nms.jumpFromGround()
            fakePlayer.nms.setJumping(true)
            val moveVec = Vector(vx, 0.42, vz)
            fakePlayer.nms.setDeltaMovement(moveVec)
            player.velocity = moveVec
        } else {
            fakePlayer.nms.setJumping(false)
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
        if (candidates.isNotEmpty()) {
            return candidates.minByOrNull { it.distanceSquared(pLoc) }!!
        }
        // 若目標周圍完全懸空（如峽谷懸崖空中），在目標旁邊選取同高度的搭橋落腳點
        val defaultOffset = offsets.first()
        return Location(world, target.x + defaultOffset[0] + 0.5, target.y.toDouble(), target.z + defaultOffset[1] + 0.5)
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

    private fun findNextBlock(world: org.bukkit.World, action: FlattenAction, currentLoc: Location, fakePlayerUuid: java.util.UUID? = null): Block? {
        val playerBlockX = currentLoc.blockX
        val playerBlockZ = currentLoc.blockZ
        val playerBlockY = currentLoc.blockY

        val fpm = com.coderxi.plugin.fakeplayer.utils.plugin.fakePlayerManager
        val onlineWorkers = fpm.fakeplayers().filter { it.player.isOnline }

        for (y in action.maxY downTo action.minY) {
            var closestBlock: Block? = null
            var minScore = Double.MAX_VALUE
            for (x in action.minX..action.maxX) {
                for (z in action.minZ..action.maxZ) {
                    if (fakePlayerUuid != null && com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.isReservedByOther(world, x, y, z, fakePlayerUuid)) {
                        continue
                    }
                    val block = world.getBlockAt(x, y, z)
                    val isMinable = isMinedBlock(block) && (!action.preserveOres || !isOreBlock(block))
                    val isLiquid = isLiquidBlock(block)
                    if (isMinable || isLiquid) {
                        var score = block.location.distanceSquared(currentLoc)
                        // 若為玩家正腳下的方塊，給予優先級懲罰，優先挖掘周圍方塊以防止挖坑掉落受困
                        if (x == playerBlockX && z == playerBlockZ && y <= playerBlockY) {
                            score += 100.0
                        }
                        // 避免挖掘任意在線協同假人正站立的腳下方塊
                        val isUnderWorker = onlineWorkers.any { w ->
                            val wLoc = w.player.location
                            x == wLoc.blockX && z == wLoc.blockZ && y == wLoc.blockY - 1
                        }
                        if (isUnderWorker) {
                            score += 200.0
                        }

                        // 水源/岩漿源優先於流動水
                        if (isLiquid) {
                            val data = block.blockData
                            if (data is org.bukkit.block.data.Levelled && data.level > 0) {
                                score += 50.0
                            }
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
                    if (isMinedBlock(b) || isLiquidBlock(b)) {
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
        com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.release(fakePlayer.uuid)
        val target = action.target ?: return
        fakePlayer.nms.doBlockBreakAction(target, ABORT)
        action.target = null
        action.progress = 0f
    }

    override fun onStop(fakePlayer: FakePlayer, action: FlattenAction) {
        com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.release(fakePlayer.uuid)
        resetMining(fakePlayer, action)
        fakePlayer.nms.setJumping(false)
        fakePlayer.player.getAttribute(Attribute.STEP_HEIGHT)?.let {
            it.baseValue = 0.6
        }
        val zeroVec = Vector(0.0, 0.0, 0.0)
        fakePlayer.nms.setDeltaMovement(zeroVec)
        fakePlayer.player.velocity = zeroVec
    }

}
