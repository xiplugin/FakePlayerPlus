package com.coderxi.plugin.fakeplayer.config

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.*

@Header("FakePlayerPlus 插件配置文件","")
class PluginConfig : OkaeriConfig() {

    @Comment("插件语言设置")
    var language: String = "zh_CN"

    @Comment("假人创建数量上限")
    @CustomKey("spawn-limit")
    val spawnLimit = SpawnLimit()
    class SpawnLimit : OkaeriConfig() {
        @Comment("全服最大假人总数")
        var server: Int = 999
        @Comment("默认的数量上限 (权限节点: fakeplayer.spawn)")
        var default: Int = 3
        @Comment("自定义权限组的数量上限 (需手动给权限组权限节点: fakeplayer.spawn.limit.<group>)")
        var groups: Map<String, Int> = hashMapOf(
            "scientist" to 10
        )
    }

    @Comment("假人自定义名称")
    val nametag = NametagConfig()
    class NametagConfig : OkaeriConfig() {
        val enable: Boolean = true
        @Comment("假人自定义名称的内容")
        val lines: List<String> = listOf(
            "血量:{fakeplayer_health_sprites}<red>[{fakeplayer_health}]",
            "等级:<green>{fakeplayer_level} <white>经验:<green>{fakeplayer_expToLevel} <white>状态:{fakeplayer_status_sprites}",
            "{fakeplayer_name}"
        )
        @Comment("假人自定义名称的最小刷新间隔",
            "每次刷新等于重新填充一次lines然后向绑定的对象发送更新数据包",
            "通过调高这个选项可以降低发包的频率来降低开销和网络带宽消耗",
            "当然这个选项也不意味着每 x tick都会进行刷新",
            "为了最大程度的降低开销,本插件采取[事件驱动]的方式进行更新(参见refreshEvents变量)")
        @CustomKey("refresh-interval-tick")
        val refreshIntervalTick: Long = 5
        @Comment("能够触发假人自定义名称刷新的事件",
            "只有在这些事件发生时才会更新假人的名称",
            "事件列表请参考插件源码中的FakePlayerEvent")
        @CustomKey("refresh-events")
        val refreshEvents: List<String> = listOf(
            "Damage", "RegainHealth", "ExpChange", "LevelChange"
        )

    }

    @Comment("假人死亡时动作 NONE|QUIT|RESPAWN|RESPAWN_BACK")
    @CustomKey("on-death-action")
    val onDeathAction: OnDeathAction = OnDeathAction.RESPAWN_BACK


}