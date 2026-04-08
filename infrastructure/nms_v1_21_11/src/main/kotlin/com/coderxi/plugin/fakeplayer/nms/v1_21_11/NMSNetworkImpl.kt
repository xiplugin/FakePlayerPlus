package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSNetwork
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.network.FakeConnection
import net.minecraft.server.network.CommonListenerCookie
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.net.InetAddress

class NMSNetworkImpl(address: InetAddress, private val plugin: JavaPlugin) : NMSNetwork {

    private val connection = FakeConnection(address)

    private lateinit var serverGamePacketListener: NMSServerGamePacketListener

    override fun placeNewPlayer(player: Player): NMSServerGamePacketListener {
        val server = player.server
        val handle = (player as CraftPlayer).handle
        val serverHandle = (server as CraftServer).handle
        val cookie = CommonListenerCookie.createInitial(player.profile, false)

        serverHandle.placeNewPlayer(connection, handle, cookie)

        return NMSServerGamePacketListenerImpl(server.server, connection, handle, cookie, plugin).apply {
            serverGamePacketListener = this
            handle.connection = this
        }
    }

}