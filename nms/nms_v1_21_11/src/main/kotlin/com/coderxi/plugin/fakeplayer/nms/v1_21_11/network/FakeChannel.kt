package com.coderxi.plugin.fakeplayer.nms.v1_21_11.network

import io.netty.channel.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress

class FakeChannel(parent: Channel?, private val address: InetAddress?): AbstractChannel(parent) {
    companion object {
        val EVENT_LOOP: EventLoop = DefaultEventLoop()
    }
    private val config: ChannelConfig = DefaultChannelConfig(this)
    private val pipeline: ChannelPipeline = FakeChannelPipeline(this)
    override fun config(): ChannelConfig { config.isAutoRead = true; return config }
    override fun doBeginRead() {}
    override fun doBind(localAddress: SocketAddress?) {}
    override fun doClose() {}
    override fun doDisconnect() {}
    override fun doWrite(`in`: ChannelOutboundBuffer) { while (`in`.current() != null) { `in`.remove() } }
    override fun isActive(): Boolean = true
    override fun isCompatible(loop: EventLoop?): Boolean = true
    override fun isOpen(): Boolean = true
    override fun pipeline(): ChannelPipeline = pipeline
    override fun localAddress0(): SocketAddress = InetSocketAddress(address, 25565)
    override fun remoteAddress0(): SocketAddress = InetSocketAddress(address, 25565)
    override fun metadata(): ChannelMetadata = ChannelMetadata(true)
    override fun eventLoop(): EventLoop = EVENT_LOOP
    override fun newUnsafe(): AbstractUnsafe = object : AbstractUnsafe() {
        override fun connect(remoteAddress: SocketAddress?, localAddress: SocketAddress?, promise: ChannelPromise) {
            safeSetSuccess(promise)
        }
    }
}