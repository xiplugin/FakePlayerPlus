package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.onPluginDisable
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakePlayerReplenishListener(private val fpm: FakePlayerManager) : Listener {

    private val tasks = ConcurrentHashMap.newKeySet<ReplenishTaskMeta>()

    data class ReplenishTaskMeta(
        val playerUuid: UUID,
        val hand: EquipmentSlot,
        val itemType: Material
    )

    init {
        val tasksConsumer = Bukkit.getServer().globalRegionScheduler.runAtFixedRate(plugin, { consume() }, 1L, 1L)
        onPluginDisable(tasksConsumer::cancel)
    }

    fun consume() {
        if (tasks.isEmpty()) return
        val iterator = tasks.iterator()
        while (iterator.hasNext()) {
            val meta = iterator.next()
            val player = Bukkit.getPlayer(meta.playerUuid)?.takeIf { it.isOnline }
            if (player == null) {
                iterator.remove()
                continue
            }
            player.scheduler.run(plugin, {
                if (player.isOnline) {
                    player.replenish(meta.hand, meta.itemType)
                }
            }, null)
            iterator.remove()
        }
    }

    @EventHandler
    fun onItemConsume(event: PlayerItemConsumeEvent) {
        val fakePlayer = fpm.get(event.player.uniqueId)?.takeIf { it.settings.autoReplenish } ?: return
        tasks.add(ReplenishTaskMeta(fakePlayer.uuid, event.hand, event.item.type))
    }

    @EventHandler
    fun onItemBreak(event: PlayerItemBreakEvent) {
        val fakePlayer = fpm.get(event.player.uniqueId)?.takeIf { it.settings.autoReplenish } ?: return
        val hand = fakePlayer.getConsumingHand(event.brokenItem.type) ?: return
        tasks.add(ReplenishTaskMeta(fakePlayer.uuid, hand, event.brokenItem.type))
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val fakePlayer = fpm.get(event.player.uniqueId)?.takeIf { it.settings.autoReplenish } ?: return
        tasks.add(ReplenishTaskMeta(fakePlayer.uuid, event.hand, event.block.type))
    }

    @EventHandler
    fun onProjectileLaunch(event: PlayerLaunchProjectileEvent) {
        val fakePlayer = fpm.get(event.player.uniqueId)?.takeIf { it.settings.autoReplenish } ?: return
        val hand = fakePlayer.getConsumingHand(event.itemStack.type) ?: return
        tasks.add(ReplenishTaskMeta(fakePlayer.uuid, hand, event.itemStack.type))
    }

    private fun FakePlayer.getConsumingHand(itemType: Material): EquipmentSlot? {
        val mainHandItem = nms.mainHandItem
        val offHandItem = nms.offHandItem
        return when {
            mainHandItem.type == itemType && offHandItem.type == Material.AIR -> EquipmentSlot.HAND
            offHandItem.type == itemType && mainHandItem.type == Material.AIR -> EquipmentSlot.OFF_HAND
            else -> null
        }
    }

    private fun Player.replenish(hand: EquipmentSlot, itemType: Material) {
        val currentHandItem = inventory.getItem(hand)
        if (currentHandItem.type == itemType && currentHandItem.amount > 0) return
        val fromSlot = inventory.first(itemType)
        if (fromSlot == -1) return
        val item = inventory.getItem(fromSlot) ?: return
        inventory.setItem(fromSlot, ItemStack(Material.AIR))
        inventory.setItem(hand, item.clone())
    }

}