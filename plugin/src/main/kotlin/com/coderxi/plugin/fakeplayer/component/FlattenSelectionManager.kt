package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.utils.isFolia
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.tlp
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

enum class ChestRole {
    OUTPUT, // 產物存放箱
    TOOL    // 工具補給箱
}

data class FlattenSelection(
    var pos1: Block? = null,
    var pos2: Block? = null,
    val outputChests: MutableList<Block> = mutableListOf(),
    val toolChests: MutableList<Block> = mutableListOf(),
    val selectedWorkers: MutableSet<UUID> = mutableSetOf(),
    var preserveOres: Boolean = false,
    var pickupItems: Boolean = true,
    var autoDeposit: Boolean = true
) {
    val isComplete: Boolean get() = pos1 != null && pos2 != null && pos1?.world == pos2?.world

    val chestBlocks: List<Block> get() = outputChests + toolChests
    val chestBlock: Block? get() = outputChests.firstOrNull() ?: toolChests.firstOrNull()

    val blockCount: Int get() {
        val p1 = pos1 ?: return 0
        val p2 = pos2 ?: return 0
        return (abs(p1.x - p2.x) + 1) * (abs(p1.y - p2.y) + 1) * (abs(p1.z - p2.z) + 1)
    }

    val minX: Int get() = minOf(pos1!!.x, pos2!!.x)
    val maxX: Int get() = maxOf(pos1!!.x, pos2!!.x)
    val minY: Int get() = minOf(pos1!!.y, pos2!!.y)
    val maxY: Int get() = maxOf(pos1!!.y, pos2!!.y)
    val minZ: Int get() = minOf(pos1!!.z, pos2!!.z)
    val maxZ: Int get() = maxOf(pos1!!.z, pos2!!.z)

    val sizeX: Int get() = abs(maxX - minX) + 1
    val sizeY: Int get() = abs(maxY - minY) + 1
    val sizeZ: Int get() = abs(maxZ - minZ) + 1

    companion object {
        fun getAssociatedBlocks(block: Block): List<Block> {
            try {
                val blockData = block.blockData
                if (blockData is org.bukkit.block.data.type.Chest) {
                    val type = blockData.type
                    if (type != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                        val facing = blockData.facing
                        val otherFace = if (type == org.bukkit.block.data.type.Chest.Type.LEFT) {
                            when (facing) {
                                BlockFace.NORTH -> BlockFace.EAST
                                BlockFace.SOUTH -> BlockFace.WEST
                                BlockFace.WEST -> BlockFace.NORTH
                                BlockFace.EAST -> BlockFace.SOUTH
                                else -> null
                            }
                        } else {
                            when (facing) {
                                BlockFace.NORTH -> BlockFace.WEST
                                BlockFace.SOUTH -> BlockFace.EAST
                                BlockFace.WEST -> BlockFace.SOUTH
                                BlockFace.EAST -> BlockFace.NORTH
                                else -> null
                            }
                        }
                        if (otherFace != null) {
                            val otherBlock = block.getRelative(otherFace)
                            return listOf(block, otherBlock)
                        }
                    }
                }
            } catch (_: Exception) {
            }
            return listOf(block)
        }
    }

    fun addChestBlock(block: Block, role: ChestRole = ChestRole.OUTPUT): Boolean {
        val targetList = if (role == ChestRole.TOOL) toolChests else outputChests
        val otherList = if (role == ChestRole.TOOL) outputChests else toolChests
        val associated = getAssociatedBlocks(block)

        // 若已在另一種類別清單中，先將其移出
        for (assoc in associated) {
            otherList.removeIf { it.world == assoc.world && it.x == assoc.x && it.y == assoc.y && it.z == assoc.z }
        }

        // 檢查是否已在目標清單中
        for (existing in targetList) {
            for (assoc in associated) {
                if (existing.world == assoc.world && existing.x == assoc.x && existing.y == assoc.y && existing.z == assoc.z) {
                    return false
                }
            }
        }
        targetList.add(associated.first())
        return true
    }

    fun removeChestBlock(index: Int, role: ChestRole): Boolean {
        val targetList = if (role == ChestRole.TOOL) toolChests else outputChests
        if (index in targetList.indices) {
            targetList.removeAt(index)
            return true
        }
        return false
    }

    fun switchChestRole(index: Int, currentRole: ChestRole): Boolean {
        val srcList = if (currentRole == ChestRole.TOOL) toolChests else outputChests
        val dstList = if (currentRole == ChestRole.TOOL) outputChests else toolChests
        if (index in srcList.indices) {
            val b = srcList.removeAt(index)
            dstList.add(b)
            return true
        }
        return false
    }

    fun clearChestBlocks(role: ChestRole? = null) {
        if (role == null || role == ChestRole.OUTPUT) outputChests.clear()
        if (role == null || role == ChestRole.TOOL) toolChests.clear()
    }
}

object FlattenSelectionManager : Listener {

    private val selections = ConcurrentHashMap<UUID, FlattenSelection>()
    private val selectingPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val selectingChestPlayers = ConcurrentHashMap<UUID, ChestRole>()
    private val chestHighlightPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private var isParticleTaskStarted = false

    private val repository = com.coderxi.plugin.fakeplayer.repository.FlattenRepository()

    fun isSelecting(player: Player): Boolean = selectingPlayers.contains(player.uniqueId)
    fun isSelectingChest(player: Player): Boolean = selectingChestPlayers.containsKey(player.uniqueId)
    fun getSelectingChestRole(player: Player): ChestRole? = selectingChestPlayers[player.uniqueId]
    fun isHighlightingChests(player: Player): Boolean = chestHighlightPlayers.contains(player.uniqueId)

    fun setHighlightingChests(player: Player, enable: Boolean) {
        if (enable) {
            chestHighlightPlayers.add(player.uniqueId)
            ensureParticleTask()
        } else {
            chestHighlightPlayers.remove(player.uniqueId)
        }
    }

    fun toggleHighlightingChests(player: Player): Boolean {
        val current = isHighlightingChests(player)
        setHighlightingChests(player, !current)
        return !current
    }

    fun ensureParticleTask() {
        if (isParticleTaskStarted) return
        isParticleTaskStarted = true

        val renderParticlesForPlayer = { player: Player ->
            if (player.isOnline && isHighlightingChests(player)) {
                val selection = getSelection(player)
                if (selection != null) {
                    // 渲染產物存放箱 (金色/火焰光環)
                    for (cb in selection.outputChests) {
                        if (cb.world != player.world) continue
                        val loc = cb.location
                        if (loc.distanceSquared(player.location) <= 48 * 48) {
                            val blocks = FlattenSelection.getAssociatedBlocks(cb)
                            for (b in blocks) {
                                val cx = b.x + 0.5
                                val cy = b.y + 0.5
                                val cz = b.z + 0.5
                                player.spawnParticle(Particle.WAX_ON, cx, cy, cz, 4, 0.35, 0.20, 0.35, 0.0)
                                player.spawnParticle(Particle.FLAME, cx, cy + 0.35, cz, 1, 0.05, 0.05, 0.05, 0.01)
                            }
                        }
                    }

                    // 渲染工具補給箱 (綠色/綠寶石光環)
                    for (cb in selection.toolChests) {
                        if (cb.world != player.world) continue
                        val loc = cb.location
                        if (loc.distanceSquared(player.location) <= 48 * 48) {
                            val blocks = FlattenSelection.getAssociatedBlocks(cb)
                            for (b in blocks) {
                                val cx = b.x + 0.5
                                val cy = b.y + 0.5
                                val cz = b.z + 0.5
                                player.spawnParticle(Particle.HAPPY_VILLAGER, cx, cy, cz, 4, 0.35, 0.20, 0.35, 0.0)
                                player.spawnParticle(Particle.END_ROD, cx, cy + 0.35, cz, 1, 0.05, 0.05, 0.05, 0.01)
                            }
                        }
                    }
                }
            }
        }

        if (isFolia) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                if (chestHighlightPlayers.isEmpty()) return@runAtFixedRate
                for (uuid in chestHighlightPlayers) {
                    val player = Bukkit.getPlayer(uuid) ?: continue
                    if (!player.isOnline) {
                        chestHighlightPlayers.remove(uuid)
                        continue
                    }
                    player.scheduler.run(plugin, { _ ->
                        renderParticlesForPlayer(player)
                    }, null)
                }
            }, 500L, 500L, java.util.concurrent.TimeUnit.MILLISECONDS)
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                if (chestHighlightPlayers.isEmpty()) return@Runnable
                for (uuid in chestHighlightPlayers) {
                    val player = Bukkit.getPlayer(uuid) ?: continue
                    if (!player.isOnline) {
                        chestHighlightPlayers.remove(uuid)
                        continue
                    }
                    renderParticlesForPlayer(player)
                }
            }, 10L, 10L)
        }
    }

    fun startSelection(player: Player) {
        selectingPlayers.add(player.uniqueId)
        selectingChestPlayers.remove(player.uniqueId)
        getOrCreateSelection(player)
    }

    fun startChestSelection(player: Player, role: ChestRole = ChestRole.OUTPUT) {
        selectingChestPlayers[player.uniqueId] = role
        selectingPlayers.remove(player.uniqueId)
        setHighlightingChests(player, true)
        getOrCreateSelection(player)
    }

    fun cancelSelection(player: Player) {
        selectingPlayers.remove(player.uniqueId)
        selectingChestPlayers.remove(player.uniqueId)
        selections.remove(player.uniqueId)
        repository.deleteSelection(player.uniqueId)
    }

    fun clearSelection(player: Player) {
        val sel = selections[player.uniqueId]
        if (sel != null) {
            sel.pos1 = null
            sel.pos2 = null
            repository.saveSelection(player.uniqueId, sel)
        }
    }

    fun clearChestBlocks(player: Player, role: ChestRole? = null) {
        val sel = selections[player.uniqueId]
        if (sel != null) {
            sel.clearChestBlocks(role)
            repository.saveSelection(player.uniqueId, sel)
        }
    }

    fun removeChestBlock(player: Player, index: Int, role: ChestRole): Boolean {
        val selection = selections[player.uniqueId] ?: return false
        val removed = selection.removeChestBlock(index, role)
        if (removed) {
            repository.saveSelection(player.uniqueId, selection)
        }
        return removed
    }

    fun switchChestRole(player: Player, index: Int, currentRole: ChestRole): Boolean {
        val selection = selections[player.uniqueId] ?: return false
        val switched = selection.switchChestRole(index, currentRole)
        if (switched) {
            repository.saveSelection(player.uniqueId, selection)
        }
        return switched
    }

    fun stopChestSelection(player: Player) {
        selectingChestPlayers.remove(player.uniqueId)
    }

    fun stopSelectingMode(player: Player) {
        selectingPlayers.remove(player.uniqueId)
        selectingChestPlayers.remove(player.uniqueId)
    }

    fun getSelection(player: Player): FlattenSelection? {
        var sel = selections[player.uniqueId]
        if (sel == null) {
            sel = repository.loadSelection(player.uniqueId)
            if (sel != null) {
                selections[player.uniqueId] = sel
            }
        }
        return sel
    }

    fun getOrCreateSelection(player: Player): FlattenSelection =
        selections.computeIfAbsent(player.uniqueId) {
            repository.loadSelection(player.uniqueId) ?: FlattenSelection()
        }

    fun saveSelection(player: Player) {
        val sel = selections[player.uniqueId] ?: return
        repository.saveSelection(player.uniqueId, sel)
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (event.hand != EquipmentSlot.HAND) return

        // 綁定箱子模式 (支援產物箱/工具箱多箱綁定，Shift+點擊退出)
        if (isSelectingChest(player)) {
            if (player.isSneaking) {
                stopChestSelection(player)
                player.sendMessage(tlp("fakeplayer.flatten.chest.exit"))
                event.isCancelled = true
                return
            }

            val clicked = event.clickedBlock ?: return
            if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                if (clicked.state is Container) {
                    val selection = getOrCreateSelection(player)
                    val role = getSelectingChestRole(player) ?: ChestRole.OUTPUT
                    val added = selection.addChestBlock(clicked, role)
                    if (added) {
                        repository.saveSelection(player.uniqueId, selection)
                        val total = if (role == ChestRole.TOOL) selection.toolChests.size else selection.outputChests.size
                        val roleMsgKey = if (role == ChestRole.TOOL) "fakeplayer.flatten.chest.tool.added" else "fakeplayer.flatten.chest.output.added"
                        player.sendMessage(tlp(roleMsgKey, clicked.x, clicked.y, clicked.z, total))
                    } else {
                        val total = if (role == ChestRole.TOOL) selection.toolChests.size else selection.outputChests.size
                        player.sendMessage(tlp("fakeplayer.flatten.chest.already-bound", total))
                    }
                } else {
                    player.sendMessage(tlp("fakeplayer.flatten.chest.invalid"))
                }
            }
            return
        }

        // 兩點選區模式 (Shift+點擊退出)
        if (isSelecting(player)) {
            if (player.isSneaking) {
                stopSelectingMode(player)
                player.sendMessage(tlp("fakeplayer.flatten.cancel"))
                event.isCancelled = true
                return
            }

            val clicked = event.clickedBlock ?: return
            val selection = getOrCreateSelection(player)
            when (event.action) {
                Action.LEFT_CLICK_BLOCK -> {
                    selection.pos1 = clicked
                    repository.saveSelection(player.uniqueId, selection)
                    event.isCancelled = true
                    player.sendMessage(tlp("fakeplayer.flatten.pos1.set", clicked.x, clicked.y, clicked.z))
                    if (selection.isComplete) {
                        player.sendMessage(tlp("fakeplayer.flatten.area.ready", selection.blockCount))
                    }
                }
                Action.RIGHT_CLICK_BLOCK -> {
                    selection.pos2 = clicked
                    repository.saveSelection(player.uniqueId, selection)
                    event.isCancelled = true
                    player.sendMessage(tlp("fakeplayer.flatten.pos2.set", clicked.x, clicked.y, clicked.z))
                    if (selection.isComplete) {
                        player.sendMessage(tlp("fakeplayer.flatten.area.ready", selection.blockCount))
                    }
                }
                else -> {}
            }
        }
    }

}
