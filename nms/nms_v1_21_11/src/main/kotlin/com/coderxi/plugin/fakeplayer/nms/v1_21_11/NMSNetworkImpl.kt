package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSNetwork
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.network.FakeConnection
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import java.net.InetAddress

open class NMSNetworkImpl(address: InetAddress) : NMSNetwork {

    private val connection = FakeConnection(address)

    override fun placeNewPlayer(player: Player): NMSServerGamePacketListener {
        val server = player.server
        val handle = (player as CraftPlayer).handle
        val serverHandle = (server as CraftServer).handle
        val cookie = CommonListenerCookie.createInitial(player.profile, false)
        serverHandle.placeNewPlayer(connection, handle, cookie)
        val serverGamePacketListener: Any = newServerGamePacketListener(server.server, connection, handle, cookie)
        handle.connection = serverGamePacketListener as ServerGamePacketListenerImpl
        return serverGamePacketListener as NMSServerGamePacketListener
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> newServerGamePacketListener(
        server: DedicatedServer,
        connection: FakeConnection,
        handle: ServerPlayer,
        cookie: CommonListenerCookie
    ): T where T : ServerGamePacketListenerImpl, T : NMSServerGamePacketListener {
        return NMSServerGamePacketListenerImpl(server, connection, handle, cookie) as T
    }

}