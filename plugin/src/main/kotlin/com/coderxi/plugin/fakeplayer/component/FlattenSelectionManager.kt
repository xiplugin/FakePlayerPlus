package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.utils.tlp
import org.bukkit.Location
import org.bukkit.block.Block
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
    var pos2: Block? = null
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

    fun countSolidBlocks(): Int {
        val p1 = pos1 ?: return 0
        val world = p1.world ?: return 0
        var count = 0
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val b = world.getBlockAt(x, y, z)
                    if (!b.type.isAir && b.type.hardness >= 0f) {
                        count++
                    }
                }
            }
        }
        return count
    }
}

object FlattenSelectionManager : Listener {

    private val selections = ConcurrentHashMap<UUID, FlattenSelection>()
    private val selectingPlayers = ConcurrentHashMap.newKeySet<UUID>()

    fun isSelecting(player: Player): Boolean = selectingPlayers.contains(player.uniqueId)

    fun startSelection(player: Player) {
        selectingPlayers.add(player.uniqueId)
        if (!selections.containsKey(player.uniqueId)) {
            selections[player.uniqueId] = FlattenSelection()
        }
    }

    fun cancelSelection(player: Player) {
        selectingPlayers.remove(player.uniqueId)
        selections.remove(player.uniqueId)
    }

    fun clearSelection(player: Player) {
        selections.remove(player.uniqueId)
    }

    fun stopSelectingMode(player: Player) {
        selectingPlayers.remove(player.uniqueId)
    }

    fun getSelection(player: Player): FlattenSelection? = selections[player.uniqueId]

    fun setSelectionByRadius(player: Player, center: Location, radius: Int, heightAbove: Int = 3): FlattenSelection {
        val world = center.world
        val cx = center.blockX
        val cy = center.blockY
        val cz = center.blockZ
        val p1 = world.getBlockAt(cx - radius, cy, cz - radius)
        val p2 = world.getBlockAt(cx + radius, cy + heightAbove, cz + radius)
        val selection = FlattenSelection(p1, p2)
        selections[player.uniqueId] = selection
        return selection
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!isSelecting(player)) return
        if (event.hand != EquipmentSlot.HAND) return

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
