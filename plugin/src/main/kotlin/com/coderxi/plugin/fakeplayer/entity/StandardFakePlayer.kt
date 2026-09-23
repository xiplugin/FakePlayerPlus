package com.coderxi.plugin.fakeplayer.entity

import com.coderxi.plugin.fakeplayer.action.ActionControllerImpl
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.model.PlayerDetail
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.utils.bukkit.SkinFetcher
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.ExperienceOrb
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class StandardFakePlayer(
    override val name: String,
    override val uuid: UUID,
    creatorUuid: UUID? = null,
    ownerUuids: Collection<UUID> = emptyList(),
    private val initialTextures: PlayerTextures? = null,
    override val settings: FakePlayerSettings
) : FakePlayer {

    init {
        settings.bind(this)
    }

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
        if (settings.xpNoCooldown) {
            nms.takeXpDelay = 0
            if (plugin.server.currentTick % 20 == 0) {
                nms.takeOrbs(player.location.getNearbyEntitiesByType(ExperienceOrb::class.java,2.0))
            }
        }
        if (settings.infiniteFoodLevel) {
            if (plugin.server.currentTick % 20 == 0) {
                player.foodLevel = 20
                player.saturation = 20f
            }
        }
    }

    override var ticking: Boolean = false

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