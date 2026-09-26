package com.coderxi.plugin.fakeplayer.config

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings.*
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment

class FakePlayerSettingsConfig : FakePlayerSettings, OkaeriConfig() {

        @Comment("是否开启实体碰撞", "提示：本插件不会覆盖其他插件的碰撞行为。如果你安装了其他基于计分板的插件，需要你在对应插件手动关闭碰撞。比如 TAB 插件就需要设置：scoreboard-teams.enable-collision: false")
        @Comment("Whether to enable entity collision", "Tips: This plugin does not override collision behaviors from other plugins. If you have other scoreboard-based plugins installed, you need to manually disable collision in those plugins. For example, in the TAB plugin, you need to set: scoreboard-teams.enable-collision: false")
        @Comment("是否开启实体碰撞")
        @Comment("Whether to enable entity collision")
        override var collidable = true

        @Comment("是否开启拾取物品")
        @Comment("Whether to enable picking up items")
        override var pickupItems: Boolean = true

        @Comment("是否开启无敌状态")
        @Comment("Whether to enable invulnerability status")
        override var invulnerable: Boolean = false

        @Comment("是否开启无限饱食度")
        @Comment("Whether to enable infinite food level")
        override var infiniteFoodLevel: Boolean = false

        @Comment("是否开启自动补货")
        @Comment("Whether to enable auto-replenish")
        override var autoReplenish: Boolean = true

        @Comment("是否开启自动钓鱼")
        @Comment("Whether to enable auto-fishing")
        override var autoFish: Boolean = true

        @Comment("假人渲染距离")
        @Comment("Simulation Distance")
        override var simulationDistance: Int = 10

        @Comment("是否开启经验吸收无冷却")
        @Comment("Whether to disable the XP pickup cooldown")
        override var xpNoCooldown: Boolean = true

        @Comment("是否开启自动切换工具")
        @Comment("Whether to automatically equip the best tool")
        override var autoEquipTool: Boolean = false

        @Comment("死亡不掉落")
        @Comment("Keep inventory")
        override var keepInventory = true

        @Comment("=======================================================")
        @Comment("下列设置默认不包含在[fakeplayer.basic]权限中, 允许玩家修改需给予权限[fakeplayer.settings.变量名]")
        @Comment("These settings are not included in the [fakeplayer.basic] permission by default. Players must be granted the fakeplayer.settings.<settingName> permission to modify them.")
        @Comment("=======================================================")

        @Comment("交互时动作")
        @Comment("Action to perform when interacted with")
        @Comment("NONE, OPEN_INVENTORY, OPEN_ENDER_CHEST, OPEN_SETTINGS_UI")
        override var interactedAction = InteractedAction.OPEN_INVENTORY

        @Comment("蹲下交互时动作")
        @Comment("Action to perform when interacted with while sneaking")
        @Comment("NONE, OPEN_INVENTORY, OPEN_ENDER_CHEST, OPEN_SETTINGS_UI")
        override var shiftInteractedAction = InteractedAction.OPEN_ENDER_CHEST

        @Comment("死亡时动作")
        @Comment("Action on fake player death")
        @Comment("NONE, QUIT, RESPAWN, RESPAWN_BACK")
        override var deathAction = DeathAction.RESPAWN_BACK

        @Comment("跟随玩家退出")
        @Comment("Follow player to quit")
        override var followQuiting = true

        @Comment("延迟x秒再跟随退出(若在x秒内重新上线则假人不会退出)")
        @Comment("Delay x seconds before following to quit (If player logs back in within x seconds, the fake player will not be removed)")
        override var followQuitingDelay = 3

    }