package com.coderxi.plugin.fakeplayer.utils.bukkit

import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

object SkinFetcher {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private suspend fun getOnlinePlayerIdByName(name: String): String? {
        val request = HttpRequest.newBuilder().GET().uri(URI.create("https://api.mojang.com/users/profiles/minecraft/$name")).build()
        val response = runCatching { httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await() }.getOrNull() ?: return null
        if (response.statusCode() != 200) return null
        val uuid = JsonParser.parseString(response.body()).asJsonObject.get("id")
        if (uuid.isJsonNull) return null
        return uuid.asString
    }

    private suspend fun getOnlinePlayerTexturesById(id: String): PlayerTextures? {
        val request = HttpRequest.newBuilder().GET().uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/$id?unsigned=false")).build()
        val response = runCatching { httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await() }.getOrNull() ?: return null
        if (response.statusCode() != 200) return null
        val profile = JsonParser.parseString(response.body()).asJsonObject
        val properties = profile.get("properties")
        if (properties.isJsonNull) return null
        val textures = (properties.asJsonArray.find{ property -> property.asJsonObject.get("name").asString == "textures" } ?: return null)
        val value = textures.asJsonObject.get("value").asString
        val signature = textures.asJsonObject.get("signature").asString
        return PlayerTextures(value, signature)
    }

    private val texturesCache: Cache<String, PlayerTextures> = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build()
    suspend fun getPlayerTexturesByName(name: String?, cache: Boolean = false): PlayerTextures? {
        if (name == null) return null
        val cachedSkin = texturesCache.getIfPresent(name)
        if (cachedSkin != null) return cachedSkin
        val skin = withContext(Dispatchers.IO) {
            getOnlinePlayerIdByName(name)?.let { getOnlinePlayerTexturesById(it) }
        }
        if (cache && skin != null) {
            texturesCache.put(name, skin)
        }
        return skin
    }

}