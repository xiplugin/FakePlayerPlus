package com.coderxi.plugin.fakeplayer.nms.v1_21_11.network

import io.netty.channel.*
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.EventExecutorGroup
import java.net.SocketAddress

@Suppress("OVERRIDE_DEPRECATION")
class FakeChannelPipeline(private val channel: Channel) : ChannelPipeline {
    override fun channel() = channel
    override fun names(): MutableList<String?> = mutableListOf()
    override fun toMap(): MutableMap<String?, ChannelHandler?> = mutableMapOf()
    override fun iterator() = toMap().entries.iterator()
    private fun release(msg: Any?): ChannelPipeline { ReferenceCountUtil.release(msg);return this}
    override fun fireChannelRead(msg: Any?) = release(msg)
    override fun write(msg: Any?) = succeededFuture.also { ReferenceCountUtil.release(msg) }
    override fun write(msg: Any?, promise: ChannelPromise): ChannelPromise = promise.setSuccess().also { ReferenceCountUtil.release(msg) }
    override fun writeAndFlush(msg: Any?) = write(msg)
    override fun writeAndFlush(msg: Any?, promise: ChannelPromise) = write(msg, promise)
    private val succeededFuture: ChannelFuture = DefaultChannelPromise(channel).apply { setSuccess() }
    override fun bind(local: SocketAddress?) = succeededFuture
    override fun connect(remote: SocketAddress?) = succeededFuture
    override fun connect(remote: SocketAddress?, local: SocketAddress?) = succeededFuture
    override fun disconnect() = succeededFuture
    override fun close() = succeededFuture
    override fun deregister() = succeededFuture
    override fun bind(l: SocketAddress?, p: ChannelPromise): ChannelPromise = p.setSuccess()
    override fun connect(r: SocketAddress?, p: ChannelPromise): ChannelPromise = p.setSuccess()
    override fun connect(r: SocketAddress?, l: SocketAddress?, p: ChannelPromise): ChannelPromise = p.setSuccess()
    override fun disconnect(p: ChannelPromise): ChannelPromise = p.setSuccess()
    override fun close(p: ChannelPromise): ChannelPromise = p.setSuccess()
    override fun deregister(p: ChannelPromise): ChannelPromise = p.setSuccess()
    override fun addFirst(name: String?, handler: ChannelHandler?) = this
    override fun addFirst(group: EventExecutorGroup?, name: String?, handler: ChannelHandler?) = this
    override fun addFirst(vararg handlers: ChannelHandler?) = this
    override fun addFirst(group: EventExecutorGroup?, vararg handlers: ChannelHandler?) = this
    override fun addLast(name: String?, handler: ChannelHandler?) = this
    override fun addLast(group: EventExecutorGroup?, name: String?, handler: ChannelHandler?) = this
    override fun addLast(vararg handlers: ChannelHandler?) = this
    override fun addLast(group: EventExecutorGroup?, vararg handlers: ChannelHandler?) = this
    override fun addBefore(base: String?, name: String?, handler: ChannelHandler?) = this
    override fun addBefore(g: EventExecutorGroup?, b: String?, n: String?, h: ChannelHandler?) = this
    override fun addAfter(base: String?, name: String?, handler: ChannelHandler?) = this
    override fun addAfter(g: EventExecutorGroup?, b: String?, n: String?, h: ChannelHandler?) = this
    override fun remove(handler: ChannelHandler?) = this
    override fun remove(name: String?): ChannelHandler? = null
    override fun <T : ChannelHandler?> remove(handlerType: Class<T?>?): T? = null
    override fun removeFirst(): ChannelHandler? = null
    override fun removeLast(): ChannelHandler? = null
    override fun replace(oldH: ChannelHandler?, newN: String?, newH: ChannelHandler?) = this
    override fun replace(oldN: String?, newN: String?, newH: ChannelHandler?): ChannelHandler? = null
    override fun <T : ChannelHandler?> replace(oldT: Class<T?>?, newN: String?, newH: ChannelHandler?): T? = null
    override fun first() = null
    override fun firstContext() = null
    override fun last() = null
    override fun lastContext() = null
    override fun get(name: String?) = null
    override fun <T : ChannelHandler?> get(handlerType: Class<T?>?) = null
    override fun context(handler: ChannelHandler?) = null
    override fun context(name: String?) = null
    override fun context(handlerType: Class<out ChannelHandler?>?) = null
    override fun fireChannelRegistered() = this
    override fun fireChannelUnregistered() = this
    override fun fireChannelActive() = this
    override fun fireChannelInactive() = this
    override fun fireExceptionCaught(cause: Throwable?) = this
    override fun fireUserEventTriggered(event: Any?) = this
    override fun fireChannelReadComplete() = this
    override fun fireChannelWritabilityChanged() = this
    override fun flush() = this
    override fun read(): ChannelOutboundInvoker = this
    override fun newPromise(): ChannelPromise = DefaultChannelPromise(channel)
    override fun newProgressivePromise(): ChannelProgressivePromise = DefaultChannelProgressivePromise(channel)
    override fun newSucceededFuture(): ChannelFuture = succeededFuture
    override fun newFailedFuture(cause: Throwable?): ChannelFuture = DefaultChannelPromise(channel).apply { setFailure(cause) }
    override fun voidPromise(): ChannelPromise = channel.voidPromise()
}