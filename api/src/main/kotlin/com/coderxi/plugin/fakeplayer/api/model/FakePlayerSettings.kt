package com.coderxi.plugin.fakeplayer.api.model

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer

data class FakePlayerSettings(
    var collidable: Boolean,
    var pickupItems: Boolean,
    var invulnerable: Boolean,
    var autoReplenish: Boolean,
    var autoFish: Boolean,
    var simulationDistance: Int,
    var xpNoCooldown: Boolean,
) {
    companion object {
        fun from(fakePlayer: FakePlayer) = FakePlayerSettings(
            fakePlayer.collidable,
            fakePlayer.pickupItems,
            fakePlayer.invulnerable,
            fakePlayer.autoReplenish,
            fakePlayer.autoFish,
            fakePlayer.simulationDistance,
            fakePlayer.xpNoCooldown
        )
    }
}