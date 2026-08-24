package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.utils.tlp
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.Container
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
    var chestBlock: Block? = null
) {
    val isComplete: Boolean get() = pos1 != null && pos2 != null && pos1?.world == pos2?.world

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
}

object FlattenSelectionManager : Listener {

    private val selections = ConcurrentHashMap<UUID, FlattenSelection>()
    private val selectingPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val selectingChestPlayers = ConcurrentHashMap.newKeySet<UUID>()

    fun isSelecting(player: Player): Boolean = selectingPlayers.contains(player.uniqueId)
    fun isSelectingChest(player: Player): Boolean = selectingChestPlayers.contains(player.uniqueId)

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

    fun clearChestBlock(player: Player) {
        selections[player.uniqueId]?.chestBlock = null
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

        val clicked = event.clickedBlock ?: return

        // 綁定箱子模式
        if (isSelectingChest(player)) {
            if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                if (clicked.state is Container) {
                    val selection = selections.getOrPut(player.uniqueId) { FlattenSelection() }
                    selection.chestBlock = clicked
                    selectingChestPlayers.remove(player.uniqueId)
                    player.sendMessage(tlp("fakeplayer.flatten.chest.set", clicked.x, clicked.y, clicked.z))
                } else {
                    player.sendMessage(tlp("fakeplayer.flatten.chest.invalid"))
                }
            }
            return
        }

        // 兩點選區模式
        if (isSelecting(player)) {
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
