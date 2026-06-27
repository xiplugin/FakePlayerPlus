package com.coderxi.plugin.fakeplayer.nms.v1_21_11.network

import io.netty.channel.ChannelFutureListener
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import java.net.InetAddress

class FakeConnection(address: InetAddress) : Connection(PacketFlow.SERVERBOUND) {

    init {
        this.channel = FakeChannel(null, address)
        this.address = this.channel.remoteAddress()
        configureSerialization(this.channel.pipeline(), PacketFlow.SERVERBOUND, false, null)
    }

    override fun isConnected(): Boolean = true
    override fun send(packet: Packet<*>) {}
    override fun send(packet: Packet<*>,sendListener: ChannelFutureListener?) {}
    override fun send(packet: Packet<*>, sendListener: ChannelFutureListener?, flush: Boolean) {}
}