package com.coderxi.plugin.fakeplayer.expansion

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.utils.onPluginReload
import com.coderxi.plugin.fakeplayer.utils.tls
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FakePlayerPlaceholderExpansion(private val fpm: FakePlayerManager) : PlaceholderExpansion() {

    private val plugin get() = com.coderxi.plugin.fakeplayer.utils.plugin

    private lateinit var timeformatter: DateTimeFormatter

    private fun loadTimeFormatter() {
        timeformatter = DateTimeFormatter.ofPattern(tls("fakeplayer.var.time.format")).withZone(ZoneId.systemDefault())
    }

    init {
        loadTimeFormatter()
        onPluginReload(::loadTimeFormatter)
    }

    override fun getIdentifier() = "fakeplayer"
    override fun getAuthor() = plugin.pluginMeta.authors.joinToString(",")
    override fun getVersion() = plugin.pluginMeta.version

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val params = params.lowercase()
        // 全局变量
        if (params == "total") {
            return fpm.fakeplayersCount().toString()
        }
        if (params == "list") {
            return fpm.fakeplayers().joinToString(tls("fakeplayer.var.list.separator")) { it.name }
        }
        if (params.startsWith("list_")) {
            val parts = params.removePrefix("list_").split("_", limit = 2)
            if (parts.size != 2) return ""
            val index = parts[0].toIntOrNull() ?: return ""
            val fakePlayer = fpm.fakeplayers().getOrNull(index) ?: return ""
            return onPlaceholderRequest(fakePlayer, parts[1])
        }
        // 玩家变量
        if (params == "isfake") {
            return fpm.isFake(player.uniqueId).toString()
        }
        // 假人变量
        val fakePlayer = fpm.get(player.uniqueId) ?: return ""
        return onPlaceholderRequest(fakePlayer, params)
    }

    private fun onPlaceholderRequest(fakePlayer: FakePlayer, params: String): String? {
        if (params == "name") {
            return fakePlayer.name
        }
        if (params == "uuid") {
            return fakePlayer.uuid.toString()
        }
        if (params == "spawner") {
            return fakePlayer.spawnerName
        }
        if (params == "spawntime") {
            return timeformatter.format(Instant.ofEpochMilli(fakePlayer.spawnTime))
        }
        if (params == "actions") {
            val actions = fakePlayer.actions.getActiveActions().values
            val actionsTexts = actions.map { tls("fakeplayer.action."+it.type.name.replace("_","-").lowercase()) }
            return actionsTexts.joinToString(tls("fakeplayer.var.action.separator")).trim()
        }
        return null
    }



}