package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.onPluginDisable
import com.coderxi.plugin.fakeplayer.utils.onPluginReload
import com.coderxi.plugin.fakeplayer.utils.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class FakePlayerPingUpdater(private val fpm: FakePlayerManager) : Listener {

    private val firstPingMap = ConcurrentHashMap<UUID, Int>()

    private var pingJitterTask: ScheduledTask? = null
    private var pingInitIsFixed = true
    private var pingInitMin = -1
    private var pingInitMax = -1

    init {
        onload()
        onPluginReload(::onload)
        onPluginDisable(::dispose)
    }

    fun onload() {
        firstPingMap.clear()
        pingJitterTask?.cancel()
        pingJitterTask = null
        val b = plugin.config.behavior
        val pingInit = b.pingInit
        val pingInitRange = pingInit.split(',').mapNotNull { it.toIntOrNull() }
        pingInitMin = pingInitRange.min()
        pingInitMax = pingInitRange.max()
        pingInitIsFixed = pingInitMin == pingInitMax
        fpm.fakeplayers().forEach(::registerFirstPing)
        val pingJitter = b.pingJitter
        val pingJitterInterval = b.pingJitterInterval
        if (!pingJitter||pingJitterInterval <= 0) return
        val ticks = pingJitterInterval.toLong() * 20
        pingJitterTask = plugin.server.globalRegionScheduler.runAtFixedRate(plugin,{fpm.fakeplayers().forEach{it.pingJitter()}}, ticks, ticks)
    }

    @EventHandler
    fun onFakePlayerConnectedEvent(event: FakePlayerConnectedEvent) {
        registerFirstPing(event.fakePlayer)
    }

    private fun registerFirstPing(fakePlayer: FakePlayer) {
        val firstPing = if (pingInitIsFixed) {
            pingInitMin
        } else {
            ThreadLocalRandom.current().nextInt(pingInitMin, pingInitMax+1)
        }
        firstPingMap[fakePlayer.uuid] = firstPing
        fakePlayer.setPing(firstPing, true)
    }

    fun FakePlayer.pingJitter() {
        val firstPing = firstPingMap[uuid] ?: return
        if (firstPing < 0) return
        val random = ThreadLocalRandom.current()
        val chance = random.nextInt(100)
        var change = 0
        if (chance < 5) { // 5% 概率触发 ±5~8 ms
            change = random.nextInt(5, 9)
        } else if (chance < 15) { // 10% 概率触发 ±3~4 ms
            change = random.nextInt(3, 5)
        } else if (chance < 75) { // 60% 概率触发 ±1~2 ms
            change = random.nextInt(1, 3)
        } // 剩下的 25% 保持不动 (change = 0)
        if (change > 0) {
            if (ping > firstPing) {
                ping -= change
            } else if (ping < firstPing) {
                ping += change
            } else {
                ping += if (random.nextBoolean()) change else -change
            }
            // 边界控制，差距不超过 8
            if (ping > firstPing + 8) ping = firstPing + 8
            if (ping < firstPing - 8) ping = firstPing - 8
            if (ping < 0) ping = 0
        }
    }

    fun dispose() {
        pingJitterTask?.cancel()
        pingJitterTask = null
    }

}