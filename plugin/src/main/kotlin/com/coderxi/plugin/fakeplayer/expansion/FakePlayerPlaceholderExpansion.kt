package com.coderxi.plugin.fakeplayer.expansion

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.messages.tls
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class FakePlayerPlaceholderExpansion : PlaceholderExpansion(), PluginComponent {

    private var timeformatters: ConcurrentHashMap<Locale, DateTimeFormatter> = ConcurrentHashMap()

    override fun onReload() {
        timeformatters.clear()
    }

    override fun getIdentifier() = "fakeplayer"
    override fun getAuthor() = plugin.pluginMeta.authors.joinToString(",")
    override fun getVersion() = plugin.pluginMeta.version

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val params = params.lowercase()
        val locale = player.locale()
        // 全局变量
        if (params == "total") {
            return fpm.fakeplayersCount().toString()
        }
        if (params == "list") {
            return fpm.fakeplayers().joinToString(tls(locale,"fakeplayer.var.list.separator")) { it.name }
        }
        if (params.startsWith("list_")) {
            val parts = params.removePrefix("list_").split("_", limit = 2)
            if (parts.size != 2) return ""
            val index = parts[0].toIntOrNull() ?: return ""
            val fakePlayer = fpm.fakeplayers().getOrNull(index) ?: return ""
            return onPlaceholderRequest(locale, fakePlayer, parts[1])
        }
        // 玩家变量
        if (params == "isfake") {
            return fpm.isFake(player.uniqueId).toString()
        }
        // 假人变量
        val fakePlayer = fpm.get(player.uniqueId) ?: return ""
        return onPlaceholderRequest(locale, fakePlayer, params)
    }

    private fun onPlaceholderRequest(locale: Locale, fakePlayer: FakePlayer, params: String): String? {
        if (params == "name") {
            return fakePlayer.name
        }
        if (params == "uuid") {
            return fakePlayer.uuid.toString()
        }
        if (params == "spawner") {
            return fakePlayer.spawner.name
        }
        if (params == "spawntime") {
            return timeformatters.computeIfAbsent(locale) { DateTimeFormatter.ofPattern(tls(locale,"fakeplayer.var.time.format")).withZone(ZoneId.systemDefault()) }
                .format(Instant.ofEpochMilli(fakePlayer.spawnTime))
        }
        if (params == "actions") {
            return fakePlayer.actions.activeActions.map(plugin.globalActionRegistry::getName)
                .joinToString(tls(locale,"fakeplayer.var.action.separator")) { name -> tls(locale,"fakeplayer.action.$name") }
        }
        return null
    }



}