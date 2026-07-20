package com.coderxi.plugin.fakeplayer.config

import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.provider.invsee.OpenInvInvseeProvider
import com.coderxi.plugin.fakeplayer.provider.invsee.VanillaInvseeProvider
import com.coderxi.plugin.fakeplayer.utils.plugin
import eu.okaeri.configs.OkaeriConfig
import org.bukkit.Location

enum class DeathEventAction { NONE, QUIT, RESPAWN, RESPAWN_BACK }

enum class InvseeProviderType(val providerClass: Class<out InvseeProvider>) {
    VANILLA(VanillaInvseeProvider::class.java),
    OPENINV(OpenInvInvseeProvider::class.java)
}

enum class PreventKickingType { NEVER, SPAWNING }

class LocationConfig : OkaeriConfig() {
    var world: String = "world"
    var x: Double = 0.5
    var y: Double = 64.0
    var z: Double = 0.5
    var yaw: Float = 0.0f
    var pitch: Float = 0.0f
    fun asLocation(): Location {
        return Location(plugin.server.getWorld(world), x, y, z, yaw, pitch)
    }
}