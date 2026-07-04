package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.network.FakeConnection
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.Server

open class NMSServerImpl(server: Server) : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSServerImpl(server) {

    @Suppress("UNCHECKED_CAST")
    override fun <T> newGamePacketListener(
        server: DedicatedServer,
        connection: FakeConnection,
        handle: ServerPlayer,
        cookie: CommonListenerCookie
    ): T where T : ServerGamePacketListenerImpl, T : NMSServerGamePacketListener {
        return NMSServerGamePacketListenerImpl(server,connection,handle,cookie) as T
    }

}