package com.coderxi.plugin.fakeplayer.utils

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer.SkinInfo
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
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

    private fun getOnlinePlayerIdByName(name: String): String? {
        val request = HttpRequest.newBuilder().GET().uri(URI.create("https://api.mojang.com/users/profiles/minecraft/$name")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null
        val uuid = JsonParser.parseString(response.body()).asJsonObject.get("id")
        if (uuid.isJsonNull) return null
        return uuid.asString
    }

    private fun getOnlinePlayerSkinInfoById(id: String): SkinInfo? {
        val request = HttpRequest.newBuilder().GET().uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/$id?unsigned=false")).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null
        val profile = JsonParser.parseString(response.body()).asJsonObject
        val properties = profile.get("properties")
        if (properties.isJsonNull) return null
        val textures = (properties.asJsonArray.find{ property -> property.asJsonObject.get("name").asString == "textures" } ?: return null)
        val value = textures.asJsonObject.get("value").asString
        val signature = textures.asJsonObject.get("signature").asString
        return SkinInfo(value, signature)
    }

    private val skinInfoCache: Cache<String, SkinInfo> = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build()
    suspend fun getPlayerSkinInfoByName(name: String?, cache: Boolean = false): SkinInfo? {
        if (name == null) return null
        val cachedSkin = skinInfoCache.getIfPresent(name)
        if (cachedSkin != null) return cachedSkin
        val skin = withContext(Dispatchers.IO) {
            getOnlinePlayerIdByName(name)?.let { getOnlinePlayerSkinInfoById(it) }
        }
        if (cache && skin != null) {
            skinInfoCache.put(name, skin)
        }
        return skin
    }

}