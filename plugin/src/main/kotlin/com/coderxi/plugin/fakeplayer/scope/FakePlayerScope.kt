package com.coderxi.plugin.fakeplayer.scope

import com.coderxi.plugin.fakeplayer.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.manager.FakePlayerRegistry
import net.kyori.adventure.text.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface FakePlayerScope {

    val uniqueId: UUID

    val registry get() = FakePlayerRegistry

    fun fakeplayers(): Collection<FakePlayer>

    fun notify(message: Component)

    fun spawnAsync(name: String): CompletableFuture<FakePlayer?>

    fun tick()

    fun remove(uuid: UUID)

    fun destroy()

}