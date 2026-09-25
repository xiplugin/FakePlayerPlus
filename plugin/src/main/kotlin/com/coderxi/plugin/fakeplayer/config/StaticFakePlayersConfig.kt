package com.coderxi.plugin.fakeplayer.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.CustomKey
import org.bukkit.Bukkit
import org.bukkit.Location

class StaticFakePlayersConfig : OkaeriConfig() {

    @Comment("是否启用此功能")
    @Comment("Whether to enable the static-fakeplayers feature")
    var enabled: Boolean = false

    @Comment("延迟生成时间（单位：秒）", "等待服务器核心、地图和其它插件完全准备就绪后再生成，防止找不到世界")
    @Comment("Delay before spawning (in seconds)", "Wait until server, worlds, and other plugins are fully loaded to prevent errors")
    var delay: Long = 5

    @Comment("默认生成位置")
    @Comment("Default Spawn Location")
    @CustomKey("default-location")
    var defaultLocation: LocationConfig? = LocationConfig()
    class LocationConfig : OkaeriConfig() {
        var world: String = "world"
        var x: Double = 0.5
        var y: Double = 64.0
        var z: Double = 0.5
        var yaw: Float = 0.0f
        var pitch: Float = 0.0f
        fun asLocation(): Location {
            return Location(Bukkit.getWorld(world), x, y, z, yaw, pitch)
        }
    }

    @Comment("静态持久假人列表")
    @Comment("List of static fake players to be spawned automatically")
    @CustomKey("static-fakeplayers")
    var staticFakePlayerMetas = mutableListOf(StaticFakePlayerMeta())
    class StaticFakePlayerMeta : OkaeriConfig() {
        var name = "Npc_1"
        var skin: String? = "bot"
        var ticking: Boolean? = false
        var location: LocationConfig? = null
        var settings: FakePlayerSettingsConfig? = null
    }

}