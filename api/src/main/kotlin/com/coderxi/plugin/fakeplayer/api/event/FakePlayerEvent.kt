package com.coderxi.plugin.fakeplayer.api.event

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot

sealed class FakePlayerEvent: Event() {
    abstract val fakePlayer : FakePlayer
    fun call() = Bukkit.getPluginManager().callEvent(this)
}
/** 假人刚被初始化 (尚未建立网络连接) */
class FakePlayerPreparingEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
/** 假人已建立网络连接并注册到了假人列表 (尚未进入世界) */
class FakePlayerConnectedEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
/** 假人已进入世界 */
class FakePlayerSpawnedEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
/** 假人触发退出事件 */
data class FakePlayerQuitEvent(override val fakePlayer: FakePlayer, val reason: Component?): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
/** 假人退出事件完成之后 */
class FakePlayerQuitedEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
// 从Player转发的事件
class FakePlayerRespawnEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
data class FakePlayerDeathEvent(override val fakePlayer: FakePlayer, val location: Location): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
data class FakePlayerDamageEvent(override val fakePlayer: FakePlayer, val finalDamage: Double): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
data class FakePlayerRegainHealthEvent(override val fakePlayer: FakePlayer, val amount: Double): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
class FakePlayerExpChangeEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
class FakePlayerLevelChangeEvent(override val fakePlayer: FakePlayer): FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
data class FakePlayerMoveEvent(override val fakePlayer: FakePlayer, val from: Location, val to: Location) : FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
// 与Player交互的事件
data class FakePlayerInteractedEvent(override val fakePlayer: FakePlayer, val player: Player, val hand: EquipmentSlot) : FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}
data class FakePlayerWatchedEvent(override val fakePlayer: FakePlayer, val player: Player) : FakePlayerEvent() {
    companion object { val hl = HandlerList() }
    override fun getHandlers() = hl
}