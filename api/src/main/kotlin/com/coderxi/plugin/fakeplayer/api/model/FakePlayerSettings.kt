package com.coderxi.plugin.fakeplayer.api.model

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

data class FakePlayerSettings(
    var collidable: Boolean?,
    var pickupItems: Boolean?,
    var invulnerable: Boolean?,
    var infiniteFoodLevel: Boolean?,
    var autoReplenish: Boolean?,
    var autoFish: Boolean?,
    var simulationDistance: Int?,
    var xpNoCooldown: Boolean?,
    var autoEquipTool: Boolean?,
    var interactedAction: InteractedAction?,
    var shiftInteractedAction: InteractedAction?,
) {
    enum class InteractedAction {
        NONE,
        OPEN_INVENTORY,
        OPEN_ENDER_CHEST,
        OPEN_SETTINGS_UI
    }
    companion object {
        fun from(fakePlayer: FakePlayer) = FakePlayerSettings(
            fakePlayer.collidable,
            fakePlayer.pickupItems,
            fakePlayer.invulnerable,
            fakePlayer.infiniteFoodLevel,
            fakePlayer.autoReplenish,
            fakePlayer.autoFish,
            fakePlayer.simulationDistance,
            fakePlayer.xpNoCooldown,
            fakePlayer.autoEquipTool,
            fakePlayer.interactedAction,
            fakePlayer.shiftInteractedAction,
        )
    }
}