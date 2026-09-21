package com.coderxi.plugin.fakeplayer.dialog

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*
import com.coderxi.plugin.fakeplayer.utils.FormDialog
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.launch
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.tl
import com.coderxi.plugin.fakeplayer.utils.tlp
import com.coderxi.plugin.fakeplayer.utils.tls
import org.bukkit.entity.Player

class FakePlayerSettingsDialog(fakePlayer: FakePlayer, val viewer: Player): FormDialog(
    tl("fakeplayer.gui.settings.title",fakePlayer.name)
) {

    init {
        boolSingleOption(fakePlayer::collidable, tl("fakeplayer.gui.settings.collidable"), permissions = listOf(SETTINGS_COLLIDABLE.value, BASIC.value))
        boolSingleOption(fakePlayer::pickupItems, tl("fakeplayer.gui.settings.pickup-items"), permissions = listOf(SETTINGS_PICKUP_ITEMS.value, BASIC.value))
        boolSingleOption(fakePlayer::invulnerable, tl("fakeplayer.gui.settings.invulnerable"), permissions = listOf(SETTINGS_INVULNERABLE.value, BASIC.value))
        boolSingleOption(fakePlayer::infiniteFoodLevel, tl("fakeplayer.gui.settings.infinite-food-level"), permissions = listOf(SETTINGS_INFINITE_FOOD_LEVEL.value, BASIC.value))
        boolSingleOption(fakePlayer::autoReplenish, tl("fakeplayer.gui.settings.auto-replenish"), permissions = listOf(SETTINGS_AUTO_REPLENISH.value, BASIC.value))
        boolSingleOption(fakePlayer::autoFish, tl("fakeplayer.gui.settings.auto-fish"), permissions = listOf(SETTINGS_AUTO_FISH.value, BASIC.value))
        numberRange(fakePlayer::simulationDistance, tl("fakeplayer.gui.settings.simulation-distance"), "%s: %s"+tls("fakeplayer.gui.unit.chunk") ,
            permissions = listOf(SETTINGS_SIMULATION_DISTANCE.value, BASIC.value),
            start = 1,
            end = if (viewer.hasPermission(ADMIN)) 32 else plugin.server.simulationDistance
        )
        boolSingleOption(fakePlayer::xpNoCooldown, tl("fakeplayer.gui.settings.xp-no-cooldown"), permissions = listOf(SETTINGS_XP_NO_COOLDOWN.value, BASIC.value))
        boolSingleOption(fakePlayer::autoEquipTool, tl("fakeplayer.gui.settings.auto-equip-tool"), permissions = listOf(SETTINGS_AUTO_EQUIP_TOOL.value, BASIC.value))
        submitButton {
            viewer.sendMessage(tlp("fakeplayer.gui.settings.submit.success", fakePlayer.name))
            launch { plugin.fakePlayerManager.saveSettings(fakePlayer) }
        }
    }

}