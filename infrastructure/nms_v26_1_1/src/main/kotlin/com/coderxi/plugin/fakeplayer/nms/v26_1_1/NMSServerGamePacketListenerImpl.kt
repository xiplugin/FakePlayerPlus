package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi.Companion.javaPlugin as plugin
import com.coderxi.plugin.fakeplayer.network.FakeConnection
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie

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

}