package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer.BlockBreakActionType
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.server.FakePlayerAdvancements
import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.chat.ChatRenderer
import net.kyori.adventure.text.Component
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.server.PlayerAdvancements
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ParticleStatus
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.ChatVisiblity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.lang.reflect.Field
import java.nio.file.Paths
import kotlin.math.ceil
import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi.Companion.javaPlugin as plugin

open class NMSServerPlayerImpl(override val player: Player) : NMSServerPlayer {

    protected val handle: ServerPlayer get() = (player as CraftPlayer).handle
    protected val server get() = handle.level().server

    override val x: Double get() = handle.x
    override val y: Double get() = handle.y
    override val z: Double get() = handle.z
    override var xo: Double get() = handle.xo; set(v) { handle.xo = v }
    override var yo: Double get() = handle.yo; set(v) { handle.yo = v }
    override var zo: Double get() = handle.zo; set(v) { handle.zo = v }
    override var xRot: Float get() = handle.xRot; set(v) { handle.xRot = v }
    override var yRot: Float get() = handle.yRot; set(v) { handle.yRot = v }

    override var xxa: Float get() = handle.xxa; set(v) {handle.xxa=v}
    override var yya: Float get() = handle.yya; set(v) {handle.yya=v}
    override var zza: Float get() = handle.zza; set(v) {handle.zza=v}

    override val tickCount: Int get() = handle.tickCount
    override val onGround: Boolean get() = handle.onGround
    override val isUsingItem: Boolean get() = handle.isUsingItem
    override val mainHandItem: ItemStack get() = handle.mainHandItem.asBukkitMirror()
    override val offHandItem: ItemStack get() =  handle.offhandItem.asBukkitMirror()
    override val blockReachDistance: Double get() = handle.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)
    override val entityReachDistance: Double get() = handle.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)

    override fun doTick() = handle.doTick()
    override fun absMoveTo(x: Double, y: Double, z: Double, yRot: Float, xRot: Float) = handle.absSnapTo(x, y, z, yRot, xRot)
    override fun setDeltaMovement(vector: Vector) { handle.deltaMovement = Vec3(vector.x, vector.y, vector.z) }
    override fun startRiding(entity: Entity, force: Boolean, triggerEvents: Boolean): Boolean = handle.startRiding((entity as CraftEntity).handle,force,triggerEvents)
    override fun stopRiding() = handle.stopRiding()
    override fun drop(allStack: Boolean) = handle.drop(allStack)
    override fun drop(slot: Int, throwRandomly: Boolean, retainOwnership: Boolean) { handle.drop(handle.inventory.removeItem(slot, handle.inventory.getItem(slot).count), throwRandomly, retainOwnership) }
    override fun jumpFromGround() = handle.jumpFromGround()
    override fun setJumping(jumping: Boolean) { handle.isJumping = jumping }
    override fun respawn() { handle.connection.handleClientCommand(ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)) }
    override fun swapItemWithOffhand() { handle.connection.handlePlayerAction(ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,BlockPos(0, 0, 0),Direction.DOWN)) }
    override fun disableAdvancements() { advancements = FakePlayerAdvancements(server.getFixerUpper(), server.playerList, server.advancements, Paths.get(System.getProperty("java.io.tmpdir")), handle) }
    override fun setupClientOptions() { handle.updateOptions(ClientInformation(
            "en_us",
            Bukkit.getViewDistance(),
            ChatVisiblity.SYSTEM,
            false,
            0x7f,
            HumanoidArm.RIGHT,
            false,
            true,
            ParticleStatus.MINIMAL
    ))}
    override fun setTextures(value: String?, signature: String?) {
        val playerProfile = player.playerProfile
        if (value == null) {
            playerProfile.removeProperty("textures")
        } else {
            playerProfile.setProperty(ProfileProperty("textures", value, signature))
        }
        player.playerProfile = playerProfile
    }

    override fun copyTextures(target: Player) {
        val texturesProperty = target.playerProfile.properties.firstOrNull { it.name == "textures" }
        if (texturesProperty != null) {
            setTextures(texturesProperty.value, texturesProperty.signature)
        } else {
            setTextures(null, null)
        }
    }

    override fun resetLastActionTime() = handle.resetLastActionTime()

    private val playerTeam by lazy { PlayerTeam(dummyScoreboard, "${plugin.name}_${player.uniqueId}").apply { players.add(player.name) } }
    private var playerTeamPacket: Packet<*>? = null

    override var dummyNametagVisibility: Boolean
        get() = playerTeam.nameTagVisibility == Team.Visibility.ALWAYS
        set(b) {
            playerTeam.nameTagVisibility = if (b) Team.Visibility.ALWAYS else Team.Visibility.NEVER
            playerTeamPacket = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true)
        }

    override var dummyCollidable: Boolean
        get() = playerTeam.collisionRule == Team.CollisionRule.ALWAYS
        set(b) {
            player.isCollidable = b
            playerTeam.collisionRule = if (b) Team.CollisionRule.ALWAYS else Team.CollisionRule.NEVER
            playerTeamPacket = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerTeam, true)
        }

    override fun dummyNotify(targets: Collection<Player>) {
        if (playerTeamPacket!=null) targets.forEach { target ->  target.sendPacket(playerTeamPacket!!) }
    }

    override fun getDestroyProgress(target: Block): Float {
        val block = target as CraftBlock
        return block.nms.getDestroyProgress(handle,handle.level(), block.position)
    }

    override fun doBlockBreakAction(target: Block, type: BlockBreakActionType) {
        val block = target as CraftBlock
        val nmsAction = when (type) {
            BlockBreakActionType.START -> ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
            BlockBreakActionType.ABORT -> ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
            BlockBreakActionType.STOP -> ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
        }
        handle.gameMode.handleBlockBreakAction(
            block.position,
            nmsAction,
            Direction.UP,
            handle.level().maxY,
            -1
        )
    }

    override fun useItem(type: EquipmentSlot, onSuccess: (() -> Unit)?) {
        val hand = when (type) { EquipmentSlot.HAND -> InteractionHand.MAIN_HAND; EquipmentSlot.OFF_HAND -> InteractionHand.OFF_HAND else -> throw Exception("Invalid equipment slot (Only HAND/OFF_HAND).") }
        val stack = handle.getItemInHand(hand)
        if (stack.isEmpty) return
        val level = handle.level()
        val hitResult = handle.getRayTrace(ceil(blockReachDistance).toInt(), ClipContext.Fluid.NONE)
        when (hitResult.type) {
            HitResult.Type.MISS -> {}
            HitResult.Type.BLOCK -> {
                val blockHit = hitResult as BlockHitResult
                val isTooHigh = blockHit.blockPos.y >= level.maxY - (if (blockHit.direction == Direction.UP) 1 else 0)
                if (isTooHigh || !level.mayInteract(handle, blockHit.blockPos)) return
                if (handle.gameMode.useItemOn(handle,level,stack, hand ,blockHit).consumesAction()) {
                    handle.swing(hand)
                    onSuccess?.invoke()
                    return
                }
            }
            HitResult.Type.ENTITY -> {
                val entityHit = hitResult as EntityHitResult
                val entity = entityHit.entity
                val relativePos = entityHit.location.subtract(entity.x, entity.y, entity.z)
                if (entity.interactAt(handle, relativePos, hand).consumesAction()) {
                    handle.swing(hand)
                    onSuccess?.invoke()
                    return
                }
                if (handle.interactOn(entity, hand).consumesAction()) {
                    handle.swing(hand)
                    onSuccess?.invoke()
                    return
                }
            }
        }
        if (handle.gameMode.useItem(handle,level,stack, hand).consumesAction()) {
            handle.swing(hand)
            onSuccess?.invoke()
        }
    }

    override fun releaseUsingItem() {
        handle.releaseUsingItem()
    }

    var advancements: PlayerAdvancements?
        get() = advancementsField?.get(handle) as? PlayerAdvancements
        set(value) { advancementsField?.set(handle, value) }

    @Suppress("UnstableApiUsage", "OverrideOnly")
    override fun chat(msg: String) {
        val message = Component.text(msg)
        val signedMessage = PlayerChatMessage.unsigned(handle.uuid, msg).adventureView()
        val viewers = Bukkit.getOnlinePlayers().plus(Bukkit.getConsoleSender()).toSet()
        val event = io.papermc.paper.event.player.AsyncChatEvent(true, player, viewers, ChatRenderer.defaultRenderer(), message, message, signedMessage)
        Bukkit.getAsyncScheduler().runNow(plugin) {
            val isAllowed = event.callEvent()
            if (isAllowed && !event.isCancelled) {
                val renderer = event.renderer()
                val finalMessage = event.message()
                val displayName = event.player.displayName()
                for (audience in event.viewers()) {
                    val renderedComponent = renderer.render(player, displayName, finalMessage, audience)
                    audience.sendMessage(renderedComponent)
                }
            }
        }
    }

    companion object {
        private val advancementsField: Field? = runCatching { ServerPlayer::class.java.getDeclaredField("advancements").apply { isAccessible = true } }.getOrNull()
        fun Player.sendPacket(packet: Packet<*>) {
            (this as CraftPlayer).handle.connection.send(packet)
        }
        private val dummyScoreboard  = Scoreboard()
    }
}
