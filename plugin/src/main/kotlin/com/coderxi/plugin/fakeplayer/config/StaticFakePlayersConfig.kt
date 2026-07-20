package com.coderxi.plugin.fakeplayer.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.CustomKey
import com.coderxi.plugin.fakeplayer.config.FakePlayerPlusPluginConfig.FakePlayerSettingsConfig

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

    @Comment("静态持久假人列表")
    @Comment("List of static fake players to be spawned automatically")
    @CustomKey("static-fakeplayers")
    var staticFakePlayerMetas = mutableListOf(StaticFakePlayerMeta())

    class StaticFakePlayerMeta : OkaeriConfig() {
        var name = "Npc_1"
        var skin: String? = "bot"
        var location: LocationConfig? = null
        var ticking: Boolean? = null
        var settings: FakePlayerSettingsConfig? = null
    }
}