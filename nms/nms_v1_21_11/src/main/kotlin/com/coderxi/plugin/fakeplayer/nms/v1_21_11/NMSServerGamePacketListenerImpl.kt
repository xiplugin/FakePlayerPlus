package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.network.FakeConnection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.phys.Vec3

open class NMSServerGamePacketListenerImpl(
    server: DedicatedServer,
    connection: FakeConnection,
    open val handle: ServerPlayer,
    cookie: CommonListenerCookie
) : ServerGamePacketListenerImpl(server, connection, handle, cookie), NMSServerGamePacketListener {

    @Volatile private var latency = 0
    override fun latency() = latency
    override fun latency(value: Int) { latency = value }

    override fun send(packet: Packet<*>) = when (packet) {
        is ClientboundSetEntityMotionPacket -> handleClientboundSetEntityMotionPacket(packet)
        is ClientboundRespawnPacket -> handleClientboundRespawnPacket(packet)
        is ClientboundKeepAlivePacket -> handleClientboundKeepAlivePacket(packet)
        else -> Unit
    }

    // 玩家被击退的动作由客户端完成, 假人没有客户端因此手动完成这个动作
    // 击退在发包后会被服务端还原, 因此在假人下一次移动前再应用, 与客户端收到速度包后的下一刻移动一致
    // Knockback is restored right after the packet is sent, so apply it before the fake player's next movement tick, like a client does on its next tick
    @Volatile protected var pendingMotion: Vec3? = null

    open fun handleClientboundSetEntityMotionPacket(packet: ClientboundSetEntityMotionPacket) {
        if (packet.id != player.id) return
        pendingMotion = packet.movement
    }

    fun applyPendingMotion() {
        val motion = pendingMotion ?: return
        pendingMotion = null
        player.lerpMotion(motion)
    }

    // 玩家重生时需要完成维度改变，否则玩家将处于无敌状态
    open fun handleClientboundRespawnPacket(packet: ClientboundRespawnPacket) {
        player.hasChangedDimension()
    }

    // 一些服务端核心需要回复keepAlive保持连接
    open fun handleClientboundKeepAlivePacket(packet: ClientboundKeepAlivePacket) {
        handleKeepAlive(ServerboundKeepAlivePacket(packet.id))
    }

}