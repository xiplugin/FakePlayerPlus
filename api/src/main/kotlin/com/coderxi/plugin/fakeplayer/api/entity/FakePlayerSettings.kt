package com.coderxi.plugin.fakeplayer.api.entity

interface FakePlayerSettings {

    var collidable: Boolean
    var pickupItems: Boolean
    var invulnerable: Boolean
    var infiniteFoodLevel: Boolean
    var autoReplenish: Boolean
    var autoFish: Boolean
    var simulationDistance: Int
    var xpNoCooldown: Boolean
    var autoEquipTool: Boolean
    var keepInventory: Boolean
    var interactedAction: InteractedAction
    var shiftInteractedAction: InteractedAction
    enum class InteractedAction {
        NONE,
        OPEN_INVENTORY,
        OPEN_ENDER_CHEST,
        OPEN_SETTINGS_UI
    }
    var deathAction: DeathAction
    enum class DeathAction {
        NONE,
        QUIT,
        RESPAWN,
        RESPAWN_BACK
    }
    var keepingMode: KeepingMode
    enum class KeepingMode {
        ALWAYS,
        FOLLOW_SPAWNER,
        FOLLOW_SPAWNER_QUIT
    }

    fun sync(that: FakePlayerSettings) {
        that.collidable = collidable
        that.pickupItems = pickupItems
        that.invulnerable = invulnerable
        that.infiniteFoodLevel = infiniteFoodLevel
        that.autoReplenish = autoReplenish
        that.autoFish = autoFish
        that.simulationDistance = simulationDistance
        that.xpNoCooldown = xpNoCooldown
        that.autoEquipTool = autoEquipTool
        that.keepInventory = keepInventory
        that.interactedAction = interactedAction
        that.shiftInteractedAction = shiftInteractedAction
        that.deathAction = deathAction
        that.keepingMode = keepingMode
    }

    fun bind(fakePlayer: FakePlayer){
    }

}