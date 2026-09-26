package com.coderxi.plugin.fakeplayer.dialog

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings.*
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*
import com.coderxi.plugin.fakeplayer.command.permission.hasPermission
import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.utils.bukkit.SimpleDialog
import com.coderxi.plugin.fakeplayer.utils.coroutine.launch
import com.coderxi.plugin.fakeplayer.utils.messages.sendLocalizedMessage
import com.coderxi.plugin.fakeplayer.utils.messages.tl
import com.coderxi.plugin.fakeplayer.utils.messages.tls
import org.bukkit.entity.Player

class FakePlayerSettingsDialog(fakePlayer: FakePlayer, val viewer: Player): SimpleDialog(
    tl(viewer,"fakeplayer.gui.settings.title",fakePlayer.name), viewer
) {

    init {
        boolSingleOption(fakePlayer.settings::collidable, tl(viewer,"fakeplayer.gui.settings.collidable"), permission = SETTINGS_COLLIDABLE.value)
        boolSingleOption(fakePlayer.settings::pickupItems, tl(viewer,"fakeplayer.gui.settings.pickup-items"), permission = SETTINGS_PICKUP_ITEMS.value)
        boolSingleOption(fakePlayer.settings::invulnerable, tl(viewer,"fakeplayer.gui.settings.invulnerable"), permission = SETTINGS_INVULNERABLE.value)
        boolSingleOption(fakePlayer.settings::infiniteFoodLevel, tl(viewer,"fakeplayer.gui.settings.infinite-food-level"), permission = SETTINGS_INFINITE_FOOD_LEVEL.value)
        boolSingleOption(fakePlayer.settings::autoReplenish, tl(viewer,"fakeplayer.gui.settings.auto-replenish"), permission = SETTINGS_AUTO_REPLENISH.value)
        boolSingleOption(fakePlayer.settings::autoFish, tl(viewer,"fakeplayer.gui.settings.auto-fish"), permission = SETTINGS_AUTO_FISH.value)
        numberRange(fakePlayer.settings::simulationDistance, tl(viewer,"fakeplayer.gui.settings.simulation-distance"), "%s: %s"+tls(viewer,"fakeplayer.gui.unit.chunk") ,
            permission = SETTINGS_SIMULATION_DISTANCE.value,
            start = 1,
            end = if (viewer.hasPermission(ADMIN)) 32 else plugin.server.simulationDistance
        )
        boolSingleOption(fakePlayer.settings::xpNoCooldown, tl(viewer,"fakeplayer.gui.settings.xp-no-cooldown"), permission = SETTINGS_XP_NO_COOLDOWN.value)
        boolSingleOption(fakePlayer.settings::autoEquipTool, tl(viewer,"fakeplayer.gui.settings.auto-equip-tool"), permission = SETTINGS_AUTO_EQUIP_TOOL.value)
        enumSingleOption(InteractedAction::class.java,fakePlayer.settings::interactedAction, tl(viewer,"fakeplayer.gui.settings.interacted-action"), optionLabelProvider = {tl(viewer,"fakeplayer.gui.var.interacted-action.${it.name}")}, permission = SETTINGS_INTERACTED_ACTION.value)
        enumSingleOption(InteractedAction::class.java,fakePlayer.settings::shiftInteractedAction, tl(viewer,"fakeplayer.gui.settings.shift-interacted-action"), optionLabelProvider = {tl(viewer,"fakeplayer.gui.var.interacted-action.${it.name}")}, permission = SETTINGS_SHIFT_INTERACTED_ACTION.value)
        enumSingleOption(DeathAction::class.java,fakePlayer.settings::deathAction,tl(viewer,"fakeplayer.gui.settings.death-action"),  optionLabelProvider = {tl(viewer,"fakeplayer.gui.var.death-action.${it.name}")}, permission = SETTINGS_DEATH_ACTION.value)
        boolSingleOption(fakePlayer.settings::keepInventory, tl(viewer,"fakeplayer.gui.settings.keep-inventory"), permission = SETTINGS_KEEP_INVENTORY.value)
        enumSingleOption(KeepingMode::class.java,fakePlayer.settings::keepingMode, tl(viewer,"fakeplayer.gui.settings.keeping-mode") , optionLabelProvider = {tl(viewer, "fakeplayer.gui.var.keeping-mode.${it.name}")}, permission = SETTINGS_KEEPING_MODE.value)
        submitButton(width = 100) {
            viewer.sendLocalizedMessage("fakeplayer.gui.settings.submit.success", fakePlayer.name)
            launch { plugin.fakePlayerManager.saveSettings(viewer, fakePlayer) }
        }
    }

}