package com.coderxi.plugin.fakeplayer.api.entity

import com.coderxi.plugin.fakeplayer.api.action.ActionHandler
import com.coderxi.plugin.fakeplayer.api.model.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.model.PlayerDetail
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import org.bukkit.entity.Player
import java.util.UUID

interface FakePlayer {

    // 基础设施
    val nms: NMSServerPlayer
    val player: Player get() = nms.player
    val actions: ActionHandler

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

    // 执行nms.doTick和actions.doTick
    fun doTick()
    // 控制是否执行刻运算
    var ticking: Boolean

    // 假人设置(持久化)
    var collidable: Boolean
    var pickupItems: Boolean
    var invulnerable: Boolean
    var autoReplenish: Boolean
    var autoFish: Boolean
    var simulationDistance: Int
    var xpNoCooldown: Boolean

    fun applySettings(settings: FakePlayerSettings)

    // nms属性
    var ping: Int
    var textures: PlayerTextures?

    // 快捷调用
    fun quit(cause: String = "")

}