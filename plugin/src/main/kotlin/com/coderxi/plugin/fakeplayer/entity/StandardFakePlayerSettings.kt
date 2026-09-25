package com.coderxi.plugin.fakeplayer.entity

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings.*

class StandardFakePlayerSettings(
    private var _collidable: Boolean,
    private var _pickupItems: Boolean,
    private var _invulnerable: Boolean,
    override var infiniteFoodLevel: Boolean,
    override var autoReplenish: Boolean,
    override var autoFish: Boolean,
    private var _simulationDistance: Int,
    override var xpNoCooldown: Boolean,
    override var autoEquipTool: Boolean,
    override var interactedAction: InteractedAction,
    override var shiftInteractedAction: InteractedAction,
    override var deathAction: DeathAction,
    override var keepInventory: Boolean,
    override var followQuiting: Boolean,
    override var followQuitingDelay: Int
) : FakePlayerSettings {

    private lateinit var fakePlayer: FakePlayer

    override var collidable get() = _collidable
        set(value) {
            fakePlayer.apply {
                player.isCollidable = value
                nms.dummyCollidable = value
                nms.dummyNotify()
            }
            _collidable = value
        }
    override var pickupItems get() = _pickupItems
        set(value) {
           fakePlayer.apply {
               player.canPickupItems = value
           }
            _pickupItems = value
        }
    override var invulnerable get() = _invulnerable
        set(value) {
            fakePlayer.apply {
                player.isInvulnerable = value
            }
            _invulnerable = value
        }
    override var simulationDistance get() = _simulationDistance
        set(value) {
            fakePlayer.apply {
                player.simulationDistance = value
            }
        }

    override fun bind(fakePlayer: FakePlayer) {
        this.fakePlayer = fakePlayer
    }

}