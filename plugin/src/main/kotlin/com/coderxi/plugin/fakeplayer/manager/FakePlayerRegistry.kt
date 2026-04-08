package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.context.PluginContext
import com.coderxi.plugin.fakeplayer.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.scope.FakePlayerScope
import org.bukkit.event.Listener
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

object FakePlayerRegistry: PluginContext, Listener {

    private val scopes = ConcurrentHashMap<UUID, FakePlayerScope>()
    private val fakeplayers = ConcurrentHashMap<UUID, FakePlayer>()

    fun scopes(): Collection<FakePlayerScope> = scopes.values
    val scopesCount: Int get() = scopes.size
    fun getScope(scopeId: UUID): FakePlayerScope? = scopes[scopeId]
    fun registerScope(scope: FakePlayerScope) { scopes[scope.uniqueId] = scope }
    fun unregisterScope(uuid: UUID) = scopes.remove(uuid)

    fun fakeplayers(): Collection<FakePlayer> = fakeplayers.values
    val fakeplayersCount: Int get() = fakeplayers.size
    fun getFakePlayer(uniqueId: UUID): FakePlayer? = fakeplayers[uniqueId]
    fun registerFakePlayer(fakePlayer: FakePlayer) {fakeplayers[fakePlayer.uniqueId] = fakePlayer}
    fun unregisterFakePlayer(uuid: UUID) = fakeplayers.remove(uuid)

    init {
        onPluginDisable {
            scopes.keys.toList().forEach { getScope(it)?.destroy() }
            scopes.clear()
            fakeplayers.values.forEach { it.quit("Plugin Disabled") }
            fakeplayers.clear()
        }
    }

}