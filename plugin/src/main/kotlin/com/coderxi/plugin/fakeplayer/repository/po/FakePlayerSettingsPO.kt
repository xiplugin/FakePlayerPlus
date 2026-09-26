package com.coderxi.plugin.fakeplayer.repository.po

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings.*
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayerSettings
import com.coderxi.plugin.fakeplayer.plugin

data class FakePlayerSettingsPO(
    var collidable: Boolean? = null,
    var pickupItems: Boolean? = null,
    var invulnerable: Boolean? = null,
    var infiniteFoodLevel: Boolean? = null,
    var autoReplenish: Boolean? = null,
    var autoFish: Boolean? = null,
    var simulationDistance: Int? = null,
    var xpNoCooldown: Boolean? = null,
    var autoEquipTool: Boolean? = null,
    var keepInventory: Boolean? = null,
    var interactedAction: InteractedAction? = null,
    var shiftInteractedAction: InteractedAction? = null,
    var deathAction: DeathAction? = null,
    var keepingMode: KeepingMode? = null,
) {

    fun toEntity(default: FakePlayerSettings = plugin.config.defaultSettings) = StandardFakePlayerSettings(
        collidable ?: default.collidable,
        pickupItems ?: default.pickupItems,
        invulnerable ?: default.invulnerable,
        infiniteFoodLevel ?: default.infiniteFoodLevel,
        autoReplenish ?: default.autoReplenish,
        autoFish ?: default.autoFish,
        simulationDistance ?: default.simulationDistance,
        xpNoCooldown ?: default.xpNoCooldown,
        autoEquipTool ?: default.autoEquipTool,
        keepInventory ?: default.keepInventory,
        interactedAction ?: default.interactedAction,
        shiftInteractedAction ?: default.shiftInteractedAction,
        deathAction ?: default.deathAction,
        keepingMode ?: default.keepingMode,
    )

    companion object {
        fun fromEntity(settings: FakePlayerSettings) = FakePlayerSettingsPO(
            settings.collidable,
            settings.pickupItems,
            settings.invulnerable,
            settings.infiniteFoodLevel,
            settings.autoReplenish,
            settings.autoFish,
            settings.simulationDistance,
            settings.xpNoCooldown,
            settings.autoEquipTool,
            settings.keepInventory,
            settings.interactedAction,
            settings.shiftInteractedAction,
            settings.deathAction,
            settings.keepingMode
        )
    }

}