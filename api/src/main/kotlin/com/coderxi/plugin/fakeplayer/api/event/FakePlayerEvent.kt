package com.coderxi.plugin.fakeplayer.api.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot

sealed class FakePlayerEvent: Event() {
    abstract val fakePlayer : FakePlayer
}
/** 假人刚被初始化 (尚未建立网络连接) */
class FakePlayerPreparingEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}
/** 假人已建立网络连接并注册到了假人列表 (尚未进入世界) */
class FakePlayerConnectedEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}
/** 假人已进入世界 */
class FakePlayerSpawnedEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}
/** 假人触发退出事件 */
data class FakePlayerQuitEvent(override val fakePlayer: FakePlayer, val reason: Component?): FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}
/** 假人完全退出 */
class FakePlayerQuitedEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}
// 从Player转发的事件
class FakePlayerRespawnEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}
data class FakePlayerDeathEvent(override val fakePlayer: FakePlayer, val location: Location): FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}
// 与Player交互的事件
data class FakePlayerInteractedEvent(override val fakePlayer: FakePlayer, val player: Player, val hand: EquipmentSlot) : FakePlayerEvent() {
    companion object { @JvmStatic val HANDLERS = HandlerList() ; @JvmStatic fun getHandlerList() = HANDLERS }
    override fun getHandlers() = HANDLERS
}