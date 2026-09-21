package com.coderxi.plugin.fakeplayer.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerInteractedEvent
import com.coderxi.plugin.fakeplayer.api.model.FakePlayerSettings.InteractedAction
import com.coderxi.plugin.fakeplayer.api.model.FakePlayerSettings.InteractedAction.*
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ADMIN
import com.coderxi.plugin.fakeplayer.command.permission.Permission.BASIC
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ENDER_CHEST
import com.coderxi.plugin.fakeplayer.command.permission.Permission.INVSEE
import com.coderxi.plugin.fakeplayer.dialog.FakePlayerSettingsDialog
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.plugin
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

class FakePlayerInteractedListener : Listener {

    val config get() = plugin.config

    @EventHandler
    fun implementInteracted(event: FakePlayerInteractedEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val fakePlayer = event.fakePlayer
        if (!fakePlayer.isOwnedBy(event.player.uniqueId) && !event.player.hasPermission(ADMIN)) return
        doInteractedAction(if (!event.player.isSneaking) fakePlayer.interactedAction else event.fakePlayer.shiftInteractedAction, event.player, fakePlayer)
    }

    private fun doInteractedAction(
        action: InteractedAction,
        player: Player,
        fakePlayer: FakePlayer
    ) {
        when (action) {
            NONE -> {}
            OPEN_INVENTORY -> {
                if (!player.hasPermission(INVSEE,BASIC)) return
                InvseeProvider.current.openInventory(player, fakePlayer.player)
                fakePlayer.player.world.playSound(fakePlayer.player.location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)
            }
            OPEN_ENDER_CHEST -> {
                if (!player.hasPermission(ENDER_CHEST,BASIC)) return
                InvseeProvider.current.openEnderChest(player, fakePlayer.player)
                fakePlayer.player.world.playSound(fakePlayer.player.location, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)
            }
            OPEN_SETTINGS_UI -> {
                FakePlayerSettingsDialog(fakePlayer, player).show(player)
            }
        }
    }

}