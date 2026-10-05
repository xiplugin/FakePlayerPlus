package com.coderxi.plugin.fakeplayer.utils.stats

import com.coderxi.plugin.fakeplayer.command.permission.Permission
import com.coderxi.plugin.fakeplayer.command.permission.hasPermission
import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.utils.messages.sendLocalizedMessage
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class UpdateChecker(
    private val name: String,
    private val currentVersion: String,
) : Listener {

    companion object {
        private const val BASE_URL = "https://plugin-version-api.minecraft.coderxi.com"
        private const val DEFAULT_DOWNLOAD_URL = "https://github.com/xiplugin/FakePlayerPlus/releases"

        private val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var latestUpdate: UpdateInfo? = null

    private val job = scope.launch {
        do {
            latestUpdate = check()
            if (latestUpdate != null) {
                plugin.server.onlinePlayers.forEach { player ->
                    if (player.hasPermission(Permission.ADMIN)) {
                        player.sendLocalizedMessage("fakeplayer.version.update",latestUpdate!!.version, latestUpdate!!.download)
                    }
                }
            }
            delay(Duration.ofHours(1).toMillis())
        } while (isActive)
    }

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!event.player.hasPermission(Permission.ADMIN)) return
        latestUpdate?.let {
            event.player.sendLocalizedMessage("fakeplayer.version.update",it.version, it.download)
        }
    }

    private suspend fun check(): UpdateInfo? {
        val request = HttpRequest.newBuilder().GET().uri(URI.create("$BASE_URL/$name.json")).timeout(Duration.ofSeconds(30)).build()
        val response = runCatching { httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await() }.getOrNull() ?: return null
        if (response.statusCode() != 200) return null
        val json = runCatching { JsonParser.parseString(response.body()).asJsonObject }.getOrNull() ?: return null
        val version = json.get("version")?.asString ?: return null
        if (!isNewer(currentVersion, version)) return null
        return UpdateInfo(
            version,
            json.get("download")?.asString ?: DEFAULT_DOWNLOAD_URL,
            json.get("changelog")?.asString.orEmpty(),
        )
    }

    private fun isNewer(current: String, latest: String): Boolean {
        if (current.equals("dev", true)) return true
        val a = current.split(".").map { it.toIntOrNull() ?: 0 }
        val b = latest.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }

    fun shutdown() {
        job.cancel()
        scope.cancel()
    }

    data class UpdateInfo(
        val version: String,
        val download: String,
        val changelog: String,
    )
}