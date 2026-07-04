package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.nms.v1_21_11.network.FakeConnection
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.configuration.GlobalConfiguration
import io.papermc.paper.connection.DisconnectionReason
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.DisconnectionDetails
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi.Companion.javaPlugin as plugin

open class NMSServerGamePacketListenerImpl(
    server: DedicatedServer,
    connection: FakeConnection,
    override val handle: ServerPlayer,
    cookie: CommonListenerCookie
) : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSServerGamePacketListenerImpl(server,connection,handle,cookie) {

    override fun handleClientboundSetEntityMotionPacket(packet: ClientboundSetEntityMotionPacket) {
        if (packet.id() != player.id) return
        player.bukkitEntity.scheduler.execute(plugin,{ player.lerpMotion(packet.movement) },null,1L)
    }

    @Suppress("UnstableApiUsage")
    override fun disconnect(details: DisconnectionDetails) {
        if (!this.cserver.isPrimaryThread) {
            super.disconnect(details)
            return
        }
        val cause = (details.disconnectionReason().orElseThrow() as DisconnectionReason).game().orElse(PlayerKickEvent.Cause.UNKNOWN) as PlayerKickEvent.Cause
        val rawLeaveMessage: Component = Component.translatable(
            "multiplayer.player.left",
            NamedTextColor.YELLOW,
            *arrayOf((if (GlobalConfiguration.get().messages.useDisplayNameInQuitMessage) handle.bukkitEntity.displayName() else Component.text(handle.scoreboardName)) as ComponentLike)
        )
        val event = PlayerKickEvent(player.bukkitEntity, PaperAdventure.asAdventure(details.reason()),
            rawLeaveMessage,
            cause
        )
        if (this.cserver.server.isRunning) {
            this.cserver.pluginManager.callEvent(event)
        }
        if (event.isCancelled) {
            return
        }
        handle.quitReason = PlayerQuitEvent.QuitReason.KICKED
        server.playerList.remove(handle, event.reason())
    }

}