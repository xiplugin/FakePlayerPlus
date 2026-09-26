package com.coderxi.plugin.fakeplayer.entity

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings.*
import com.coderxi.plugin.fakeplayer.repository.po.FakePlayerSettingsPO
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import com.google.gson.Gson

class StandardFakePlayerSettings(
    private var _collidable: Boolean,
    private var _pickupItems: Boolean,
    private var _invulnerable: Boolean,
    private var _infiniteFoodLevel: Boolean,
    private var _autoReplenish: Boolean,
    private var _autoFish: Boolean,
    private var _simulationDistance: Int,
    private var _xpNoCooldown: Boolean,
    private var _autoEquipTool: Boolean,
    private var _interactedAction: InteractedAction,
    private var _shiftInteractedAction: InteractedAction,
    private var _deathAction: DeathAction,
    private var _keepInventory: Boolean,
    private var _followQuiting: Boolean,
    private var _followQuitingDelay: Int
) : FakePlayerSettings {

    companion object: PluginComponent {
        private val gson = Gson()
        private var _overrides : FakePlayerSettingsPO? = null
        val overrides : FakePlayerSettingsPO get() = _overrides ?: gson.toJson(plugin.config.overrideSettings).let { gson.fromJson(it,FakePlayerSettingsPO::class.java) }.also { _overrides = it }
        override fun onReload() { _overrides = null }
    }

    private lateinit var fakePlayer: FakePlayer

    override var collidable
        get() = overrides.collidable ?: _collidable
        set(value) {
            fakePlayer.apply {
                player.isCollidable = value
                nms.dummyCollidable = value
                nms.dummyNotify()
            }
            _collidable = value
        }

    override var pickupItems
        get() = overrides.pickupItems ?: _pickupItems
        set(value) {
            fakePlayer.player.canPickupItems = value
            _pickupItems = value
        }

    override var invulnerable
        get() = overrides.invulnerable ?: _invulnerable
        set(value) {
            fakePlayer.player.isInvulnerable = value
            _invulnerable = value
        }

    override var infiniteFoodLevel
        get() = overrides.infiniteFoodLevel ?: _infiniteFoodLevel
        set(value) {
            _infiniteFoodLevel = value
        }

    override var autoReplenish
        get() = overrides.autoReplenish ?: _autoReplenish
        set(value) {
            _autoReplenish = value
        }

    override var autoFish
        get() = overrides.autoFish ?: _autoFish
        set(value) {
            _autoFish = value
        }

    override var simulationDistance
        get() = overrides.simulationDistance ?: _simulationDistance
        set(value) {
            fakePlayer.player.simulationDistance = value
            _simulationDistance = value
        }

    override var xpNoCooldown
        get() = overrides.xpNoCooldown ?: _xpNoCooldown
        set(value) {
            _xpNoCooldown = value
        }

    override var autoEquipTool
        get() = overrides.autoEquipTool ?: _autoEquipTool
        set(value) {
            _autoEquipTool = value
        }

    override var interactedAction
        get() = overrides.interactedAction ?: _interactedAction
        set(value) {
            _interactedAction = value
        }

    override var shiftInteractedAction
        get() = overrides.shiftInteractedAction ?: _shiftInteractedAction
        set(value) {
            _shiftInteractedAction = value
        }

    override var deathAction
        get() = overrides.deathAction ?: _deathAction
        set(value) {
            _deathAction = value
        }

    override var keepInventory
        get() = overrides.keepInventory ?: _keepInventory
        set(value) {
            _keepInventory = value
        }

    override var followQuiting
        get() = overrides.followQuiting ?: _followQuiting
        set(value) {
            _followQuiting = value
        }

    override var followQuitingDelay
        get() = overrides.followQuitingDelay ?: _followQuitingDelay
        set(value) {
            _followQuitingDelay = value
        }

    override fun bind(fakePlayer: FakePlayer) {
        this.fakePlayer = fakePlayer
    }
}