package com.coderxi.plugin.fakeplayer.api.nms

import net.kyori.adventure.text.Component
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

interface NMSServerPlayer {

    val player: Player
    // 坐标
    val x: Double
    val y: Double
    val z: Double
    // 偏移量
    var xo: Double
    var yo: Double
    var zo: Double
    // 头部角度
    var xRot: Float
    var yRot: Float
    // 坐标移动
    var xxa: Float
    var yya: Float
    var zza: Float

    /** 获取时刻计数, 尽管假人会退出游戏, 但服务器重启前这个值不会重置 */
    val tickCount: Int
    /** 判断是否在地面 */
    val onGround: Boolean
    /** 是否在使用物品 */
    val isUsingItem: Boolean
    /** 主手物品 */
    val mainHandItem: ItemStack
    /** 副手物品 */
    val offHandItem: ItemStack
    /** 方块触及距离 */
    val blockReachDistance: Double
    /** 实体触及距离 */
    val entityReachDistance: Double
    /** 执行刻运算 */
    fun doTick()
    /** 移动玩家 */
    fun absMoveTo(x: Double, y: Double, z: Double, yRot: Float, xRot: Float)
    /** 设置相对移动 */
    fun setDeltaMovement(vector: Vector)

    /** 发送消息 */
    fun chat(msg: String)

    /** 骑乘实体 */
    fun startRiding(entity: Entity, force: Boolean, triggerEvents: Boolean): Boolean
    /** 取消骑乘实体 */
    fun stopRiding()

    /** 丢弃背包 */
    fun dropInventory()

    /** 重生 */
    fun respawn()
    /** 交换主副手物品 */
    fun swapItemWithOffhand()

    /** 从地面跳起 */
    fun jumpFromGround()
    /** 设置是否跳跃中 */
    fun setJumping(jumping: Boolean)

    /** 设置不保存成就数据 */
    fun disableAdvancements()
    /** 设置客户端选项 */
    fun setupClientOptions()
    /** 设置皮肤贴图 */
    fun setTextures(value: String?, signature: String?)
    /** 复制皮肤贴图 */
    fun copyTextures(target: Player)
    /** 重设最后活跃时间 */
    fun resetLastActionTime()

    // 基于数据包的属性, 若修改基于数据包的属性,必须调用dummyNotify方法手动通知
    var dummyNametagVisibility : Boolean
    var dummyCollidable : Boolean
    fun dummyNotify(targets: Collection<Player>)

    // 协助完成动作的方法
    fun getDestroyProgress(target: Block): Float
    fun doBlockBreakAction(target: Block, type: BlockBreakActionType)
    enum class BlockBreakActionType { START, ABORT, STOP }
    fun useItem(type: EquipmentSlot, onSuccess: (() -> Unit)? = null)
    fun releaseUsingItem()

}
