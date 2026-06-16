package com.coderxi.plugin.fakeplayer.api.entity

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.api.nms.NMSNetwork
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

interface FakePlayer {

    // 基础信息
    val uuid: UUID
    val name: String
    var skin: String?
    var ownerUuids: Collection<UUID>

    // 基础nms属性
    val nmsPlayer: NMSServerPlayer
    val nmsNetwork: NMSNetwork
    val nmsConnection: NMSServerGamePacketListener

    // 进行网络连接
    fun connect(nms: NMSBridge, nmsServer: NMSServer)

    // 进行基础设置
    fun setupDefaults()

    // 基础属性转发
    val player get() = nmsPlayer.player
    val world get() = player.world
    val location: Location get() = player.location
    var ping get() = nmsConnection.ping; set(value) { nmsConnection.ping = value }

    // 基础方法转发
    fun chat(message: String) = player.chat(message)
    fun quit(cause: String) = player.kick(Component.text(cause))
    fun doTick() = nmsPlayer.doTick()
    fun requestRespawn() = nmsPlayer.requestRespawn()
    fun teleportAsync(location: Location) = player.teleportAsync(location)
    fun showVirtualNametag(player: Player, content: Component) = nmsPlayer.showVirtualNametag(player, content)
    fun updateVirtualNametag(player: Player, content: Component) = nmsPlayer.updateVirtualNametag(player, content)
    fun hideVirtualNametag(player: Player) = nmsPlayer.hideVirtualNametag(player)

}