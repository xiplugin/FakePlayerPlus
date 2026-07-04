package com.coderxi.plugin.fakeplayer.api.entity

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import java.util.UUID

interface FakePlayer {

    // 基础信息
    val name: String
    val uuid: UUID
    var skin: SkinInfo?
    data class SkinInfo (
        val textures: String?,
        val signature: String?
    )
    var settings: FakePlayerSettings
    // 关联信息
    var creatorUuid: UUID?
    var ownerUuids: MutableSet<UUID>
    val owners get() = ownerUuids.mapNotNull(Bukkit::getPlayer)
    var spawnerName: String
    var spawnerUuid: UUID
    var spawnerIp: String
    var spawnTime: Long

    // 是否执行doTick和actions.doTick
    var ticking: Boolean

    // 动作控制器
    var actions: ActionHandler

    // 完成网络连接时进行的操作
    fun onConnected(nmsPlayer: NMSServerPlayer ,nmsConnection: NMSServerGamePacketListener)

    // 调用桥接
    val nms: NMSServerPlayer
    val player get() = nms.player

    // nms属性
    var ping: Int
    fun setPing(value: Int, flush: Boolean)

    // 快捷调用
    fun quit(cause: String = "") = player.kick(Component.text(cause))

}