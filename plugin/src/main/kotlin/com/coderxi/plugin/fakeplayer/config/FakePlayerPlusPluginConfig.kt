package com.coderxi.plugin.fakeplayer.config

import com.coderxi.plugin.fakeplayer.provider.invsee.AdvancedInvseeProvider
import com.coderxi.plugin.fakeplayer.provider.invsee.InvseeProvider
import com.coderxi.plugin.fakeplayer.provider.invsee.OpenInvInvseeProvider
import com.coderxi.plugin.fakeplayer.provider.invsee.VanillaInvseeProvider
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.*

class FakePlayerPlusPluginConfig : OkaeriConfig() {

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

        @Comment("按序号生成假人名称时的前缀")
        @Comment("Prefix used when generating fake player names sequentially")
        @CustomKey("sequence-name-prefix")
        var sequenceNamePrefix = ""

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

    @Comment("假人默认设置")
    @Comment("FakePlayer default settings")
    @CustomKey("default-settings")
    var defaultSettings = FakePlayerSettingsConfig()

    @Comment("强制覆盖假人的设置，如果你希望服务器假人统一应用某个选项并不可修改，可以在这里设置")
    @Comment("Force override fake player settings. If you want all fake players on the server to use a specific setting and prevent it from being modified, configure it here.")
    @CustomKey("override-settings")
    var overrideSettings = mapOf(
        "followQuiting" to true,
        "followQuitingDelay" to 30
    )

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

        @Comment("假人已建立网络连接并注册到了假人列表 (尚未进入世界) 此阶段可以添加/register和/login方法进行认证")
        @Comment("FakePlayer connected and registered to the player list (not in world yet), At this stage, /register and /login commands can be added for authentication.")
        @Comment("e.g: /register sjkJFln1il sjkJFln1il , /login sjkJFln1il")
        var connected: List<String> = arrayListOf(
            ""
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

    @Comment("其他杂项设置")
    @Comment("Misc settings")
    var msic = MiscConfig()
    class MiscConfig : OkaeriConfig() {

        @Comment("假人背包查看器", "ADVANCED:高级(装备栏+副手+快捷栏切换)", "VANILLA:原版(不支持查看装备栏)", "OPENINV:需单独安装(装备栏+副手+合成) https://github.com/Jikoo/OpenInv/releases")
        @Comment("Fake player inventory viewer", "VANILLA: Vanilla (Does not support viewing equipment/armor slots)", "ADVANCED: Advanced 6x9 layout (Equipment slots + hotbar selector)", "OPENINV: Requires separate installation: https://github.com/Jikoo/OpenInv/releases")
        @CustomKey("invsee-type")
        var invseeType =  InvseeProviderType.ADVANCED
        enum class InvseeProviderType(val providerClass: Class<out InvseeProvider>) {
            ADVANCED(AdvancedInvseeProvider::class.java),
            VANILLA(VanillaInvseeProvider::class.java),
            OPENINV(OpenInvInvseeProvider::class.java)
        }

        @Comment("防止假人被其他插件踢掉, 这个选项用来兼容一些插件因为某些问题而踢掉假人", "NEVER:不进行任何处理 SPAWNING:创建时防止被踢出")
        @Comment("Prevent some plugins kick our fake players, enabling this option may resolve some compatibility issues with login plugins.", "NEVER / SPAWNING")
        @CustomKey("prevent-kicking")
        var preventKicking = PreventKickingType.SPAWNING
        enum class PreventKickingType { NEVER, SPAWNING }

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

        @Comment("自动注册与登录(使用随机密码) 目前只支持AuthMe系列插件，其他系列登录插件请使用lifecycle-commands.connected添加/login和/register的方式进行验证")
        @Comment("Automatically register and login (using a random password). Currently only supports AuthMe-based plugins. For other login plugins, use lifecycle-commands.connected to add /register and /login commands for authentication.")
        @CustomKey("auto-auth")
        var autoAuth = true

    }

}