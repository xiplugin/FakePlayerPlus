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

data class FlattenSelection(
    var pos1: Block? = null,
    var pos2: Block? = null,
    val chestBlocks: MutableList<Block> = mutableListOf()
) {
    val isComplete: Boolean get() = pos1 != null && pos2 != null && pos1?.world == pos2?.world

    val chestBlock: Block? get() = chestBlocks.firstOrNull()

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
            val state = block.state
            if (state is Chest) {
                val holder = state.inventory.holder
                if (holder is DoubleChest) {
                    val left = (holder.leftSide as? Chest)?.block
                    val right = (holder.rightSide as? Chest)?.block
                    if (left != null && right != null) {
                        return listOf(left, right)
                    }
                }
            }
            return listOf(block)
        }
    }

    fun addChestBlock(block: Block): Boolean {
        val associated = getAssociatedBlocks(block)
        for (existing in chestBlocks) {
            val existingAssociated = getAssociatedBlocks(existing)
            for (assoc in associated) {
                if (existingAssociated.any { it.world == assoc.world && it.x == assoc.x && it.y == assoc.y && it.z == assoc.z }) {
                    return false
                }
            }
        }
        chestBlocks.add(associated.first())
        return true
    }

    fun clearChestBlocks() {
        chestBlocks.clear()
    }
}

object FlattenSelectionManager : Listener {

    private val selections = ConcurrentHashMap<UUID, FlattenSelection>()
    private val selectingPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val selectingChestPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val chestHighlightPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private var isParticleTaskStarted = false

    fun isSelecting(player: Player): Boolean = selectingPlayers.contains(player.uniqueId)
    fun isSelectingChest(player: Player): Boolean = selectingChestPlayers.contains(player.uniqueId)
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

        val runnable = Runnable {
            if (chestHighlightPlayers.isEmpty()) return@Runnable
            for (uuid in chestHighlightPlayers) {
                val player = Bukkit.getPlayer(uuid)
                if (player == null || !player.isOnline) {
                    chestHighlightPlayers.remove(uuid)
                    continue
                }
                val selection = selections[uuid] ?: continue
                if (selection.chestBlocks.isEmpty()) continue

                for (cb in selection.chestBlocks) {
                    if (cb.world != player.world) continue
                    if (cb.location.distanceSquared(player.location) > 48 * 48) continue

                    // 支援大箱子（雙格箱）與堆疊箱子
                    val blocks = FlattenSelection.getAssociatedBlocks(cb)
                    for (b in blocks) {
                        val cx = b.x + 0.5
                        val cy = b.y + 0.5
                        val cz = b.z + 0.5
                        val hasBlockAbove = !b.getRelative(BlockFace.UP).type.isAir

                        // 在箱子四周與正面產生柔和的金色與綠色閃爍粒子
                        player.spawnParticle(Particle.HAPPY_VILLAGER, cx, cy, cz, 3, 0.35, 0.20, 0.35, 0.0)
                        player.spawnParticle(Particle.WAX_ON, cx, cy, cz, 4, 0.35, 0.20, 0.35, 0.0)

                        if (!hasBlockAbove) {
                            // 上方無遮擋時，箱頂產生微光
                            player.spawnParticle(Particle.END_ROD, cx, cy + 0.35, cz, 1, 0.1, 0.05, 0.1, 0.01)
                        } else {
                            // 上方有箱子堆疊時，將粒子散佈至側面與前方
                            player.spawnParticle(Particle.END_ROD, cx, cy, cz, 1, 0.4, 0.1, 0.4, 0.01)
                        }
                    }
                }
            }
        }

        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ -> runnable.run() }, 10L, 10L)
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, 10L, 10L)
        }
    }

    fun startSelection(player: Player) {
        selectingPlayers.add(player.uniqueId)
        selectingChestPlayers.remove(player.uniqueId)
        if (!selections.containsKey(player.uniqueId)) {
            selections[player.uniqueId] = FlattenSelection()
        }
    }

    fun startChestSelection(player: Player) {
        selectingChestPlayers.add(player.uniqueId)
        selectingPlayers.remove(player.uniqueId)
        setHighlightingChests(player, true) // 進入綁定模式時預設開啟粒子特效，方便玩家查看
        if (!selections.containsKey(player.uniqueId)) {
            selections[player.uniqueId] = FlattenSelection()
        }
    }

    fun cancelSelection(player: Player) {
        selectingPlayers.remove(player.uniqueId)
        selectingChestPlayers.remove(player.uniqueId)
        selections.remove(player.uniqueId)
    }

    fun clearSelection(player: Player) {
        selections.remove(player.uniqueId)
    }

    fun clearChestBlocks(player: Player) {
        selections[player.uniqueId]?.clearChestBlocks()
    }

    fun removeChestBlock(player: Player, index: Int): Boolean {
        val selection = selections[player.uniqueId] ?: return false
        if (index in selection.chestBlocks.indices) {
            selection.chestBlocks.removeAt(index)
            return true
        }
        return false
    }

    fun stopChestSelection(player: Player) {
        selectingChestPlayers.remove(player.uniqueId)
    }

    fun stopSelectingMode(player: Player) {
        selectingPlayers.remove(player.uniqueId)
        selectingChestPlayers.remove(player.uniqueId)
    }

    fun getSelection(player: Player): FlattenSelection? = selections[player.uniqueId]

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (event.hand != EquipmentSlot.HAND) return

        // 綁定箱子模式 (支援多箱子、大箱子依序綁定，Shift+點擊退出)
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
                    val selection = selections.getOrPut(player.uniqueId) { FlattenSelection() }
                    val added = selection.addChestBlock(clicked)
                    if (added) {
                        player.sendMessage(tlp("fakeplayer.flatten.chest.added", clicked.x, clicked.y, clicked.z, selection.chestBlocks.size))
                    } else {
                        player.sendMessage(tlp("fakeplayer.flatten.chest.already-bound", selection.chestBlocks.size))
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
            val selection = selections.getOrPut(player.uniqueId) { FlattenSelection() }
            when (event.action) {
                Action.LEFT_CLICK_BLOCK -> {
                    selection.pos1 = clicked
                    event.isCancelled = true
                    player.sendMessage(tlp("fakeplayer.flatten.pos1.set", clicked.x, clicked.y, clicked.z))
                    if (selection.isComplete) {
                        player.sendMessage(tlp("fakeplayer.flatten.area.ready", selection.blockCount))
                    }
                }
                Action.RIGHT_CLICK_BLOCK -> {
                    selection.pos2 = clicked
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
