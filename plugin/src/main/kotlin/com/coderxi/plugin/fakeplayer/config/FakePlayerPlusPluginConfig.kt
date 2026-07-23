package com.coderxi.plugin.fakeplayer.config

import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.*

class FakePlayerPlusPluginConfig : OkaeriConfig() {

    @Comment("插件语言设置")
    @Comment("Plugin language settings")
    var language: String = "en"

    @Comment("插件限制设置")
    @Comment("Plugin limit settings")
    var limit = LimitConfig()
    class LimitConfig : OkaeriConfig() {

        @Comment("全服创建数量上限")
        @Comment("Maximum number of spawns allowed across the entire server")
        @CustomKey("server-spawn")
        var serverSpawn: Int = 999

        @Comment("玩家创建数量上限 (需要权限: fakeplayer.spawn)")
        @Comment("Maximum number of spawns per player (Requires permission: fakeplayer.spawn)")
        @CustomKey("player-spawn")
        var playerSpawn: Int = 3

        @Comment("自定义创建数量权限 (需要手动设置玩家/权限组权限: fakeplayer.spawn.limit.<权限名>)")
        @Comment("Custom spawn limit permissions (Requires manual setup of player/group permission: fakeplayer.spawn.limit.<permission_name>)")
        @CustomKey("custom-spawn")
        var customSpawn: Map<String, Int> = hashMapOf("vip" to 10)

        @Comment("玩家IP创建数量上限")
        @Comment("Maximum number of spawns per player IP")
        @CustomKey("ip-spawn")
        var ipSpawn: Int = 3

        @Comment("根据服务器TPS动态调整玩家创建数量上限")
        @Comment("Dynamically adjust player spawn limits based on server TPS")
        @CustomKey("tps-adaptive")
        var tpsAdaptive = TpsAdaptiveLimitConfig()
        class TpsAdaptiveLimitConfig : OkaeriConfig() {

            @Comment("是否启用此功能")
            @Comment("Whether to enable this feature")
            var enabled = true

            @Comment("检测间隔 (单位:秒)")
            @Comment("Detection interval (in seconds)")
            var interval = 120

            @Comment("检测阈值 (检测TPS低于此值时，将逐步降低假人上限，高于此值则恢复)")
            @Comment("Detection threshold (When TPS drops below this value, the fake player limit will be gradually reduced; when above, it will recover)")
            var threshold = 17.0

            @Comment("最低假人上限")
            @Comment("Minimum allowed fake player limit")
            @CustomKey("min-count")
            var minCount = 1

        }
    }

    @Comment("假人名称功能")
    @Comment("FakePlayer naming settings")
    var name = NameConfig()
    class NameConfig : OkaeriConfig() {

        @Comment("假人名称允许的字符(正则表达式)")
        @Comment("Allowed characters for fake player names (Regular Expression)")
        @CustomKey("spawn-pattern")
        var pattern = Regex("^[a-zA-Z0-9_]+$")

    }

    @Comment("假人皮肤功能")
    @Comment("FakePlayer skin settings")
    var skin = SkinConfig()
    class SkinConfig : OkaeriConfig() {

        @Comment("如果假人未被/fp skin设置过皮肤则使用此皮肤", "NONE: 不设置皮肤 SPAWNER：跟随生成者 player1:固定为一个皮肤  player1,player2:从数组中随机设置")
        @Comment("Default skin if not set via '/fp skin'.", "NONE: None. SPAWNER: Follow spawner's skin. player1: Fixed skin. player1,player2: Random selection from array.")
        @CustomKey("default")
        var default = "SPAWNER"

    }

    @Comment("假人行为设置")
    @Comment("FakePlayer behavior settings")
    var behavior = BehaviorConfig()
    class BehaviorConfig : OkaeriConfig() {

        @Comment("假人背包查看器", "VANILLA:原版(不支持查看装备栏)", "OPENINV:需单独安装 https://github.com/Jikoo/OpenInv/releases")
        @Comment("Fake player inventory viewer", "VANILLA: Vanilla (Does not support viewing equipment/armor slots)", "OPENINV: Requires separate installation: https://github.com/Jikoo/OpenInv/releases")
        @CustomKey("invsee-type")
        var invseeType =  InvseeProviderType.VANILLA

        @Comment("假人死亡时动作","NONE:无操作 QUIT:退出 RESPAWN:重生 RESPAWN_BACK:重生并返回上一次死亡地点")
        @Comment("Action on fake player death", "NONE: No action QUIT: Quit RESPAWN: Respawn RESPAWN_BACK: Respawn and return to last death location")
        @CustomKey("death-action")
        var deathAction = DeathEventAction.RESPAWN_BACK

        @Comment("死亡不掉落")
        @Comment("Keep inventory")
        @CustomKey("keep-inventory")
        var keepInventory = true

        @Comment("防止假人被其他插件踢掉, 这个选项用来兼容一些插件因为某些问题而踢掉假人", "NEVER:不进行任何处理 SPAWNING:创建时防止被踢出")
        @Comment("Prevent some plugins kick our fake players, enabling this option may resolve some compatibility issues with login plugins.", "NEVER / SPAWNING")
        @CustomKey("prevent-kicking")
        var preventKicking = PreventKickingType.SPAWNING

        @Comment("跟随玩家退出")
        @Comment("Follow player to quit")
        @CustomKey("follow-quiting")
        var followQuiting = true

        @Comment("延迟x秒再跟随退出(若玩家在x秒内重新上线则假人不会被删除)")
        @Comment("Delay x seconds before following to quit (If player logs back in within x seconds, the fake player will not be removed)")
        @CustomKey("follow-quiting-delay")
        var followQuitingDelay = 30

        @Comment("假人ping初始值","可以设置固定值 或者用20,50表示在20-50范围内的随机值")
        @Comment("Initial ping value for fake players", "Can be a fixed value or '20,50' for a random value between 20-50")
        @CustomKey("ping-init")
        var pingInit = "20,50"

        @Comment("模拟真实ping抖动")
        @Comment("Simulate realistic ping jitter")
        @CustomKey("ping-jitter")
        var pingJitter = true

        @Comment("ping值抖动间隔 (单位:秒)")
        @Comment("Ping jitter interval (in seconds)")
        @CustomKey("ping-jitter-interval")
        var pingJitterInterval = 3

    }

    @Comment("假人默认设置")
    @Comment("FakePlayer default settings")
    @CustomKey("default-settings")
    var defaultSettings = FakePlayerSettingsConfig()
    class FakePlayerSettingsConfig : OkaeriConfig() {

        @Comment("是否开启实体碰撞", "提示：本插件不会覆盖其他插件的碰撞行为。如果你安装了其他基于计分板的插件，需要你在对应插件手动关闭碰撞。比如 TAB 插件就需要设置：scoreboard-teams.enable-collision: false")
        @Comment("Whether to enable entity collision", "Tips: This plugin does not override collision behaviors from other plugins. If you have other scoreboard-based plugins installed, you need to manually disable collision in those plugins. For example, in the TAB plugin, you need to set: scoreboard-teams.enable-collision: false")
        @Comment("是否开启实体碰撞")
        @Comment("Whether to enable entity collision")
        var collidable: Boolean = true

        @Comment("是否开启拾取物品")
        @Comment("Whether to enable picking up items")
        @CustomKey("pickup-items")
        var pickupItems: Boolean = true

        @Comment("是否开启无敌状态")
        @Comment("Whether to enable invulnerability status")
        var invulnerable: Boolean = false

        @Comment("是否开启自动补货")
        @Comment("Whether to enable auto-replenish")
        var autoReplenish: Boolean = true

        @Comment("是否开启自动钓鱼")
        @Comment("Whether to enable auto-fishing")
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
    @Comment(
        "FakePlayer lifecycle command binding",
        "(No prefix) Executed by FakePlayer itself, placeholders: {uuid} {name} {spawner_uuid} {spawner_name}",
        "[CONSOLE] Executed by console, placeholders same as above",
        "[SPAWNER] Executed by spawner, placeholders same as above",
        "[OWNERS] Executed by all owners, extra placeholders: {owner_uuid} {owner_name}"
    )
    @CustomKey("lifecycle-commands")
    var lifecycleCommands = LifecycleCommandsConfig()
    class LifecycleCommandsConfig : OkaeriConfig() {
        @Comment("假人刚被初始化 (尚未建立网络连接) ","此时无法通过假人自身执行(必须带前缀)")
        @Comment("FakePlayer just initialized (network connection not established yet)", "Cannot be executed by FakePlayer itself at this stage (prefix required)")
        var preparing: List<String> = arrayListOf(
            "[CONSOLE] /lp user {uuid} parent set bot"
        )

        @Comment("假人已建立网络连接并注册到了假人列表 (尚未进入世界)")
        @Comment("FakePlayer connected and registered to the player list (not in world yet)")
        var connected: List<String> = arrayListOf(
            "/login FAKEPLAYER111"
        )

        @Comment("假人已进入世界")
        @Comment("FakePlayer spawned into the world")
        var spawned: List<String> = arrayListOf(
            "/tell {spawner_name} Hello, I am here !"
        )

        @Comment("假人触发退出事件 (仍在世界中)")
        @Comment("FakePlayer triggered quit event (still in world)")
        var quit: List<String> = arrayListOf(
            "/tell {spawner_name} Goodbye, I am leaving now!"
        )

        @CustomKey("post-quit")
        @Comment("假人完全退出","此时无法通过假人自身执行(必须带前缀)")
        @Comment("FakePlayer completely disconnected", "Cannot be executed by FakePlayer itself at this stage (prefix required)")
        var quited: List<String> = arrayListOf(
            "[CONSOLE] /tell {spawner_name} The FakePlayer {name} you created has been removed"
        )
    }

}