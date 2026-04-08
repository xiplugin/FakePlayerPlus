package com.coderxi.plugin.fakeplayer.api.nms

import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.jetbrains.annotations.NotNull

interface NMSServerPlayer {

    /**
     * @return 返回 bukkit 的 Player 对象
     */
    @NotNull
    fun getPlayer(): Player

    /**
     * @return X 坐标
     */
    fun getX(): Double

    /**
     * @return Y 坐标
     */
    fun getY(): Double

    /**
     * @return Z 坐标
     */
    fun getZ(): Double

    /**
     * 设置 X 坐标偏移值
     *
     * @param xo 偏移值
     */
    fun setXo(xo: Double)

    /**
     * 设置 Y 坐标偏移值
     *
     * @param yo 偏移值
     */
    fun setYo(yo: Double)

    /**
     * 设置 Z 坐标偏移值
     *
     * @param zo 偏移值
     */
    fun setZo(zo: Double)

    /**
     * 执行时刻运算
     */
    fun doTick()

    /**
     * 移动玩家
     *
     * @param x    X 坐标
     * @param y    Y 坐标
     * @param z    Z 坐标
     * @param yRot 头部 Y 角度
     * @param xRot 头部 X 角度
     */
    fun absMoveTo(x: Double, y: Double, z: Double, yRot: Float, xRot: Float)

    /**
     * 获取头部 Y 角度
     *
     * @return Y 角度
     */
    fun getYRot(): Float

    /**
     * 设置头部 Y 角度
     *
     * @param yRot Y 角度
     */
    fun setYRot(yRot: Float)

    /**
     * 获取头部 X 角度
     *
     * @return 头部 X 角度
     */
    fun getXRot(): Float

    /**
     * 设置头部 X 角度
     *
     * @param xRot 头部 X 角度
     */
    fun setXRot(xRot: Float)

    /**
     * 获取 Z 坐标移动
     * @return Z 坐标移动
     */
    fun getZza(): Float

    /**
     * 设置 Z 坐标移动
     *
     * @param zza 移动距离
     */
    fun setZza(zza: Float)

    /**
     * 获取 X 坐标移动
     * @return X 坐标移动
     */
    fun getXxa(): Float

    /**
     * 设置 X 坐标移动
     *
     * @param xxa 与第三双重
     */
    fun setXxa(xxa: Float)

    /**
     * 设置相对移动
     * @param vector 相对移动
     */
    fun setDeltaMovement(@NotNull vector: Vector)

    /**
     * 骑上实体
     *
     * @param entity 实体
     * @param force  是否强制
     * @return 是否骑上
     */
    fun startRiding(entity: Entity, force: Boolean, triggerEvents: Boolean): Boolean


    /**
     * 取消骑行实体
     */
    fun stopRiding()

    /**
     * 设置不保存成就数据
     *
     * @param plugin 插件
     */
    fun disableAdvancements(@NotNull plugin: Plugin)

    /**
     * 获取时刻计数, 尽管假人会退出游戏, 但服务器重启前这个值不会重置
     *
     * @return 时刻计数
     */
    fun getTickCount(): Int

    /**
     * 丢弃物品
     *
     * @param slot  槽位
     * @param flag
     * @param flag1
     */
    fun drop(slot: Int, throwRandomly: Boolean, retainOwnership: Boolean)

    /**
     * 丢弃物品
     *
     * @param allStack 是否丢弃整组
     */
    fun drop(allStack: Boolean): Boolean

    /**
     * 重设最后活跃时间
     */
    fun resetLastActionTime()

    /**
     * 判断是否在地面
     *
     * @return 是否在地面
     */
    fun onGround(): Boolean

    /**
     * 从地面跳起
     */
    fun jumpFromGround()

    /**
     * 设置是否跳跃中
     *
     * @param jumping 是否条约中
     */
    fun setJumping(jumping: Boolean)

    /**
     * 是否在使用物品
     *
     * @return 是否在使用物品
     */
    fun isUsingItem(): Boolean

    /**
     * 设置曾玩过这个服务器
     */
    fun setPlayBefore()

    /**
     * 设置客户端选项
     */
    fun setupClientOptions()

    /**
     * 重生
     */
    fun requestRespawn()

    /**
     * 交换主副手物品
     */
    fun requestSwapItemWithOffhand()

    //向玩家展示虚拟nametag
    fun showVirtualNameTag(player: Player, content: Component)
    fun hideVirtualNameTag(player: Player)
}
