package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.event.FakePlayerConnectedEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.command.permission.Permission
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

class FakePlayerLimiter(private val fpm: FakePlayerManager) : PluginComponent, Listener {

    private val ip2Count = ConcurrentHashMap<String, Int>()

    private val limit get() = plugin.config.limit

    private var tpsLimitTask: BukkitTask? = null
    private var tpsLimitThreshold = -1.0
    private var tpsLimitMinCount = 1

    // 当TPS降低时，该值增高（例如变为 1），玩家的创建数量上限将减1
    private var tpsLimitReduction = 0

    init {
        onload()
        onPluginReload(::onload)
        onPluginDisable(::dispose)
    }

    fun onload() {
        val tpsLimit = limit.tpsAdaptive
        tpsLimitThreshold = tpsLimit.threshold
        tpsLimitMinCount = tpsLimit.minCount
        if (!tpsLimit.enabled || tpsLimitThreshold < 0 || tpsLimitMinCount < 0) {
            tpsLimitTask?.cancel()
            tpsLimitTask = null
            return
        }
        val ticks = tpsLimit.interval.toLong() * 20
        tpsLimitTask = scheduler.runTaskTimer(plugin, this::run, ticks, ticks)
    }

    fun isServerLimited(): Boolean {
        return fpm.fakeplayersCount() >= limit.serverSpawn
    }

    fun isPlayerLimited(player: Player): Boolean {
        return fpm.fakeplayersByOwnerUuid(player.uniqueId).count() >= getPlayerSpawnLimit(player)
    }

    fun getPlayerSpawnLimit(player: Player): Int {
        return limit.customSpawn.filter { (node) ->
            player.hasPermission(Permission.SPAWN_LIMIT_CUSTOM.value.replace("{node}",node))
        }.values.maxOrNull() ?: limit.playerSpawn
    }

    @EventHandler
    private fun ip2CoundIncrement(event: FakePlayerConnectedEvent) {
        ip2Count.merge(event.fakePlayer.spawnerIp, 1) { old, new -> old + new }
    }

    @EventHandler
    private fun ip2CoundDecrement(event: FakePlayerQuitedEvent) {
        val ip = event.fakePlayer.spawnerIp
        ip2Count.computeIfPresent(ip) { _, count -> if (count <= 1) null else count - 1}
    }

    fun isIpLimited(player: Player): Boolean {
        val ip = player.address?.address?.hostAddress ?: return false
        return (ip2Count[ip] ?: 0) >= limit.ipSpawn
    }

    fun isTpsAdaptiveLimited(player: Player): Boolean {
        return fpm.fakeplayersByOwnerUuid(player.uniqueId).count() >= (getPlayerSpawnLimit(player) - tpsLimitReduction)
    }

    private fun run() {
        val tps = plugin.server.tps.first()
        if (tps >= tpsLimitThreshold) {
            if (tpsLimitReduction <= 0) return
            tpsLimitReduction--
            Bukkit.getOnlinePlayers().forEach { player ->
                player.sendMessage(tlp("fakeplayer.tps-adaptive.limit-recovered", tps, getPlayerSpawnLimit(player) - tpsLimitReduction))
            }
            return
        }
        if (tpsLimitReduction >= (limit.playerSpawn-tpsLimitMinCount)) return
        tpsLimitReduction++
        Bukkit.getOnlinePlayers().forEach { player ->
            val playerMaxLimit = getPlayerSpawnLimit(player) - tpsLimitReduction
            player.sendMessage(tlp("fakeplayer.tps-adaptive.limit-decreased", tps, playerMaxLimit))
            val activeFakePlayers = fpm.fakeplayersByOwnerUuid(player.uniqueId)
            val overflowCount = activeFakePlayers.size - playerMaxLimit
            if (overflowCount > 0) {
                activeFakePlayers.sortedByDescending { it.player.lastLogin }.take(overflowCount).forEach { fakePlayer ->
                    fakePlayer.quit("Tps Adaptive Limit")
                }
            }
        }
    }

    fun dispose() {
        tpsLimitTask?.cancel()
        tpsLimitTask = null
    }

}
