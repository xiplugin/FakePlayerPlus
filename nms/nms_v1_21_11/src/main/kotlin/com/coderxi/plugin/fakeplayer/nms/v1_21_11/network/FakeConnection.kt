package com.coderxi.plugin.fakeplayer.nms.v1_21_11.network

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.network.ServerGamePacketListenerImpl
import java.net.InetAddress

class FakeConnection(address: InetAddress) : Connection(PacketFlow.SERVERBOUND) {

    init {
        val channel = FakeChannel(null, address)
        this.channel = channel
        this.address = channel.remoteAddress()
        configureSerialization(channel.pipeline(), PacketFlow.SERVERBOUND, false, null)
    }

    var packetListenerImpl : ServerGamePacketListenerImpl? = null
    override fun isConnected(): Boolean = true
    override fun send(packet: Packet<*>) { packetListenerImpl?.send(packet) }
    override fun send(packet: Packet<*>, sendListener: ChannelFutureListener?) = send(packet)
    override fun send(packet: Packet<*>, sendListener: ChannelFutureListener?, flush: Boolean) = send(packet)
}