package com.coderxi.plugin.fakeplayer.api.config

data class FakePlayerSettings(
    var collidable: Boolean,
    var pickupItems: Boolean,
    var invulnerable: Boolean,
    var autoReplenish: Boolean,
    var autoFish: Boolean,
    var autoRejoin: Boolean = false,
)