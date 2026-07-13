package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi.Companion.api
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.network.FakeConnection
import com.mojang.authlib.GameProfile
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import java.net.InetAddress
import java.util.UUID

open class NMSServerImpl(override val server: Server) : NMSServer {

    override fun newPlayer(uuid: UUID, name: String): NMSServerPlayer {
        val serverHandle = (server as CraftServer).handle
        val playerHandle = ServerPlayer(
            serverHandle.server,
            (api.nms.fromWorld(Bukkit.getWorlds()[0]).world as CraftWorld).handle,
            GameProfile(uuid, name),
            ClientInformation.createDefault()
        )
        playerHandle.bukkitEntity.loadData()
        return api.nms.fromPlayer(playerHandle.bukkitEntity)
    }

    override fun placeNewPlayer(player: Player, address: InetAddress): NMSServerGamePacketListener {
        val serverHandle = (server as CraftServer).handle
        val playerHandle = (player as CraftPlayer).handle
        val connection = FakeConnection(address)
        val cookie = CommonListenerCookie.createInitial(playerHandle.gameProfile, false)
        serverHandle.placeNewPlayer(connection, playerHandle, cookie)
        val packetListener = newGamePacketListener(serverHandle.server, connection, playerHandle, cookie) as ServerGamePacketListenerImpl
        playerHandle.connection = packetListener
        connection.packetListenerImpl = packetListener
        return packetListener as NMSServerGamePacketListener
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> newGamePacketListener(
        server: DedicatedServer,
        connection: FakeConnection,
        handle: ServerPlayer,
        cookie: CommonListenerCookie
    ): T where T : ServerGamePacketListenerImpl, T : NMSServerGamePacketListener {
        return NMSServerGamePacketListenerImpl(server, connection, handle, cookie) as T
    }

}