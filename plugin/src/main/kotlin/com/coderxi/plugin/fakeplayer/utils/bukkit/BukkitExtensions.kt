package com.coderxi.plugin.fakeplayer.utils.bukkit

import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture

val UUID_ZERO = UUID(0L, 0L)

val CommandSender.uniqueIdOrZero get() = if (this is Player) uniqueId else UUID_ZERO

val isFolia by lazy { runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isSuccess }

fun Player.teleportAsync(location: Location, sound: Sound): CompletableFuture<Boolean> =
    teleportAsync(location).thenApply { success -> success.also { if (it) location.world.playSound(location, sound, 1f, 1f) } }
fun CommandSender.assertPermission(permission: String) {
    if (!hasPermission(permission)) throw FakePlayerCommandException.NoPermissionException()
}