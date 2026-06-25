package com.coderxi.plugin.fakeplayer.config

import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.*

class FakePlayerPlusPluginConfig : OkaeriConfig() {

    @Comment("插件语言设置")
    var language: String = "zh_CN"

    @Comment("插件限制设置")
    var limit = LimitConfig()
    class LimitConfig : OkaeriConfig() {
        @Comment("全服创建数量上限")
        @CustomKey("server-spawn")
        var serverSpawn: Int = 999
        @Comment("玩家创建数量上限 (需要权限: fakeplayer.spawn)")
        @CustomKey("player-spawn")
        var playerSpawn: Int = 3
        @Comment("自定义创建数量权限 (需要手动设置玩家/权限组权限: fakeplayer.spawn.limit.<权限名>)")
        @CustomKey("custom-spawn")
        var customSpawn: Map<String, Int> = hashMapOf("vip" to 10)
        @Comment("玩家IP创建数量上限")
        @CustomKey("ip-spawn")
        var ipSpawn: Int = 3
        @Comment("根据服务器TPS动态调整玩家创建数量上限")
        @CustomKey("tps-adaptive")
        var tpsAdaptive = TpsAdaptiveLimitConfig()
        class TpsAdaptiveLimitConfig : OkaeriConfig() {
            @Comment("是否启用此功能")
            var enabled = true
            @Comment("检测间隔 (单位:秒)")
            var interval = 120
            @Comment("检测阈值 (检测TPS低于此值时，将逐步降低假人上限，高于此值则恢复)")
            var threshold = 17.0
            @Comment("最低假人上限")
            @CustomKey("min-count")
            var minCount = 1
        }
    }

    @Comment("假人名称功能")
    var name = NameConfig()
    class NameConfig : OkaeriConfig() {
        @Comment("假人名称允许的字符(正则表达式)")
        @CustomKey("spawn-pattern")
        var pattern = Regex("^[a-zA-Z0-9_]+$")
    }

    @Comment("假人行为设置")
    var behavior = BehaviorConfig()
    class BehaviorConfig : OkaeriConfig() {
        @Comment("假人背包查看器", "VANILLA:原版(不支持查看装备栏)", "OPENINV:需单独安装 https://github.com/Jikoo/OpenInv/releases")
        @CustomKey("invsee-type")
        var invseeType =  InvseeProviderType.VANILLA
        @Comment("假人死亡时动作","NONE:无操作 QUIT:退出 RESPAWN:重生 RESPAWN_BACK:重生并返回上一次死亡地点")
        @CustomKey("death-action")
        var deathAction = DeathEventAction.RESPAWN_BACK
        @Comment("跟随玩家退出")
        @CustomKey("follow-quiting")
        var followQuiting = true
        @Comment("延迟x秒再跟随退出(若玩家在x秒内重新上线则假人不会被删除)")
        @CustomKey("follow-quiting-delay")
        var followQuitingDelay = 30
        @Comment("假人ping初始值","可以设置固定值 或者用20,50表示在20-50范围内的随机值")
        @CustomKey("ping-init")
        var pingInit = "20,50"
        @Comment("模拟真实ping抖动")
        @CustomKey("ping-jitter")
        var pingJitter = true
        @Comment("ping值抖动间隔 (单位:秒)")
        @CustomKey("ping-jitter-interval")
        var pingJitterInterval = 3
    }

    @Comment("假人默认设置")
    @CustomKey("default-settings")
    var defaultSettings = FakePlayerSettingsConfig()
    class FakePlayerSettingsConfig : OkaeriConfig() {
        @Comment("是否开启实体碰撞")
        var collidable: Boolean = true
        @Comment("是否开启拾取物品")
        @CustomKey("pickup-items")
        var pickupItems: Boolean = true
        @Comment("是否开启无敌状态")
        var invulnerable: Boolean = false
        @Comment("是否开启自动补货")
        var autoReplenish: Boolean = true
        @Comment("是否开启自动钓鱼")
        var autoFish: Boolean = true
        fun clone() = FakePlayerSettings(
            collidable,
            pickupItems,
            invulnerable,
            autoReplenish,
            autoFish
        )
        fun equals2(that: FakePlayerSettings): Boolean =
            collidable==that.collidable &&
            pickupItems==that.pickupItems &&
            invulnerable==that.invulnerable &&
            autoReplenish==that.autoReplenish &&
            autoFish==that.autoFish
    }

    @Comment(
        "假人生命周期指令绑定",
        "(无前缀)假人自身执行 变量 {uuid} {name} {spawner_uuid} {spawner_name}",
        "[CONSOLE]控制台执行 变量同上",
        "[SPAWNER]创建者执行 变量同上",
        "[OWNERS]全部所有者都会执行 额外变量 {owner_uuid} {owner_name}"
    )
    @CustomKey("lifecycle-commands")
    var lifecycleCommands = LifecycleCommandsConfig()
    class LifecycleCommandsConfig : OkaeriConfig() {
        @Comment("假人刚被初始化 (尚未建立网络连接) ","此时无法通过假人自身执行(必须带前缀)")
        var preparing: List<String> = arrayListOf(
            "[CONSOLE] /lp user {uuid} parent set bot"
        )
        @Comment("假人已建立网络连接并注册到了假人列表 (尚未进入世界)")
        var connected: List<String> = arrayListOf(
            "/login FAKEPLAYER111"
        )
        @Comment("假人已进入世界")
        var spawned: List<String> = arrayListOf(
            "/tell {spawner_name} 你好，我来帮你挂机啦！"
        )
        @Comment("假人触发退出事件 (仍在世界中)")
        var quit: List<String> = arrayListOf(
            "/tell {spawner_name} 再见，我要退出啦！"
        )
        @CustomKey("post-quit")
        @Comment("假人完全退出","此时无法通过假人自身执行(必须带前缀)")
        var quited: List<String> = arrayListOf(
            "[CONSOLE] /tell {spawner_name} 你创建的假人{name}已被移除"
        )
    }

}