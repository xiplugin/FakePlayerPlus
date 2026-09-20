package com.coderxi.plugin.fakeplayer.entity

import com.coderxi.plugin.fakeplayer.action.ActionControllerImpl
import com.coderxi.plugin.fakeplayer.action.handler.*
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.model.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.model.PlayerDetail
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.coderxi.plugin.fakeplayer.utils.SkinFetcher
import com.coderxi.plugin.fakeplayer.utils.plugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class StandardFakePlayer(
    override val name: String,
    override val uuid: UUID,
    creatorUuid: UUID? = null,
    ownerUuids: Collection<UUID> = emptyList(),
    private val initialTextures: PlayerTextures? = null,
    val settings: FakePlayerSettings
) : FakePlayer {

    override lateinit var nms: NMSServerPlayer
    override val actions = ActionControllerImpl(this)
    lateinit var nmsConnection: NMSServerGamePacketListener

    override lateinit var spawner: PlayerDetail
    override var creator: PlayerDetail? = creatorUuid?.let(PlayerDetail::of)

    private val ownersMap = ownerUuids.associateWithTo(mutableMapOf(),PlayerDetail::of)
    override val owners get() = ownersMap.values
    override val hasOwner get() = ownersMap.isNotEmpty()
    override fun isOwnedBy(uuid: UUID) = ownersMap.contains(uuid)
    override fun addOwner(uuid: UUID) { ownersMap[uuid] = PlayerDetail.of(uuid) }
    override fun removeOwner(uuid: UUID) { ownersMap.remove(uuid) }

    override var spawnTime by Delegates.notNull<Long>()

    override fun doTick() {
        nms.doTick()
        actions.doTick()
        if (xpNoCooldown) {
            nms.takeXpDelay = 0
        }
    }

    override var ticking: Boolean = false

    override var collidable: Boolean
        get() = settings.collidable
        set(value) {
            player.isCollidable = value
            nms.dummyCollidable = value
            nms.dummyNotify()
            settings.collidable = value
        }
    override var pickupItems: Boolean
        get() = settings.pickupItems
        set(value) {
            player.canPickupItems = value
            settings.pickupItems = value
        }
    override var invulnerable: Boolean
        get() = settings.invulnerable
        set(value) {
            player.isInvulnerable = value
            settings.invulnerable = value
        }
    override var autoReplenish: Boolean
        get() = settings.autoReplenish
        set(value) {
            settings.autoReplenish = value
        }
    override var autoFish: Boolean
        get() = settings.autoFish
        set(value) {
            settings.autoFish = value
        }
    override var simulationDistance: Int
        get() = settings.simulationDistance
        set(value) {
            player.simulationDistance = value
            settings.simulationDistance = value
        }
    override var xpNoCooldown: Boolean
        get() = settings.xpNoCooldown
        set(value) {
            settings.xpNoCooldown = value
        }
    override var autoEquipTool: Boolean
        get() = settings.autoEquipTool
        set(value) {
            settings.autoEquipTool = value
        }

    override fun applySettings(settings: FakePlayerSettings) {
        collidable = settings.collidable
        pickupItems = settings.pickupItems
        invulnerable = settings.invulnerable
        autoReplenish = settings.autoReplenish
        autoFish = settings.autoFish
        simulationDistance = settings.simulationDistance
        xpNoCooldown = settings.xpNoCooldown
    }

    override var ping: Int
        get() = nmsConnection.latency()
        set(value) {
            nmsConnection.latency(value)
        }

    override var textures: PlayerTextures? = null
    suspend fun loadTextures() {
        if (initialTextures != null) {
            nms.setTextures(initialTextures.value, initialTextures.signature)
            return
        }
        val defaultSkin = plugin.config.skin.default.takeIf { it.isNotBlank() && !it.equals("none",true) } ?: return
        if (defaultSkin.equals("spawner", true)) {
            Bukkit.getPlayer(spawner.uuid)?.let(nms::copyTextures)
            return
        }
        val randomSkin = SkinFetcher.getPlayerTexturesByName(defaultSkin.split(',').random(), true)
        nms.setTextures(randomSkin?.value, randomSkin?.signature)
    }

    private val quitting = AtomicBoolean(false)
    override fun quit(cause: String) {
        if (!quitting.compareAndSet(false, true)) return
        player.kick(Component.text(cause))
    }

}