package com.coderxi.plugin.fakeplayer.entity

import com.coderxi.plugin.fakeplayer.action.ActionHandlerImpl
import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer.SkinInfo
import com.coderxi.plugin.fakeplayer.api.nms.*
import org.bukkit.Bukkit
import java.util.UUID

class StandardFakePlayer(
    override val name: String,
    override val uuid: UUID,
    override var creatorUuid: UUID? = null,
    override var ownerUuids: MutableSet<UUID> = mutableSetOf(),
    private var _skin: SkinInfo? = null,
    private var _settings: FakePlayerSettings
) : FakePlayer {

    override var skin: SkinInfo?
        get() = _skin;
        set(skin) {
            if (skin == null || skin.textures == null || skin.signature == null) nmsPlayer.setTextures(null, null)
            else nmsPlayer.setTextures(skin.textures!!, skin.signature!!)
            _skin = skin
        }

    override var settings: FakePlayerSettings
        get() = _settings
        set(settings) {
            player.isCollidable = settings.collidable
            nmsPlayer.dummyCollidable = settings.collidable
            nmsPlayer.dummyNotify(Bukkit.getOnlinePlayers())
            player.canPickupItems = settings.pickupItems
            player.isInvulnerable = settings.invulnerable
            settings.autoReplenish
            _settings = settings
        }

    override lateinit var spawnerUuid: UUID
    override lateinit var spawnerIp: String

    override var actions : ActionHandler = ActionHandlerImpl(this)

    private lateinit var nmsPlayer: NMSServerPlayer
    private lateinit var nmsConnection: NMSServerGamePacketListener
    override fun onConnected(nmsPlayer: NMSServerPlayer, nmsConnection: NMSServerGamePacketListener) {
        this.nmsPlayer = nmsPlayer
        this.nmsConnection = nmsConnection
        player.apply {
            isPersistent = true
            isSleepingIgnored = true
            nmsPlayer.dummyCollidable = settings.collidable
            nmsPlayer.dummyNotify(Bukkit.getOnlinePlayers())
            canPickupItems = settings.pickupItems
            isInvulnerable = settings.invulnerable
            health = 20.0
            foodLevel = 20

        }
    }

    override val nms: NMSServerPlayer get() = nmsPlayer

    override var ping: Int
        get() = nmsConnection.ping
        set(value) {nmsConnection.ping = value}
}