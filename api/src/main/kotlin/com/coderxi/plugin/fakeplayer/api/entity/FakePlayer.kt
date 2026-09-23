package com.coderxi.plugin.fakeplayer.api.entity

import com.coderxi.plugin.fakeplayer.api.action.ActionController
import com.coderxi.plugin.fakeplayer.api.model.PlayerDetail
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import org.bukkit.entity.Player
import java.util.UUID

interface FakePlayer {

    // 基础设施
    val nms: NMSServerPlayer
    val player: Player get() = nms.player
    val actions: ActionController

    // 基本信息
    val name: String
    val uuid: UUID

    // 关联信息
    val spawner: PlayerDetail
    var creator: PlayerDetail?
    val owners: Collection<PlayerDetail>
    val hasOwner: Boolean
    fun isOwnedBy(uuid: UUID): Boolean
    fun addOwner(uuid: UUID)
    fun removeOwner(uuid: UUID)
    val spawnTime: Long

    // 假人设置
    val settings: FakePlayerSettings

    // 执行nms.doTick和actions.doTick
    fun doTick()
    // 控制是否执行刻运算
    var ticking: Boolean

    // nms属性
    var ping: Int
    var textures: PlayerTextures?

    // 快捷调用
    fun quit(cause: String = "")

}