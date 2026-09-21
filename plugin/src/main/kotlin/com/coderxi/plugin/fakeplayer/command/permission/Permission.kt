package com.coderxi.plugin.fakeplayer.command.permission

enum class Permission(val value: String) {

    ADMIN("fakeplayer.admin"),
    BASIC("fakeplayer.basic"),

    HELP("fakeplayer.help"),
    RELOAD("fakeplayer.reload"),

    SPAWN("fakeplayer.spawn"),
    SPAWN_WITH_NAME("fakeplayer.spawn.name"),
    SPAWN_LIMIT_CUSTOM("fakeplayer.spawn.limit.{node}"),

    SELECT("fakeplayer.select"),

    REMOVE("fakeplayer.remove"),

    KILL("fakeplayer.kill"),
    INVSEE("fakeplayer.invsee"),
    ENDER_CHEST("fakeplayer.enderchest"),
    TP("fakeplayer.tp"),
    EXPME("fakeplayer.expme"),
    SKIN("fakeplayer.skin"),
    CMD("fakeplayer.command"),
    CHAT("fakeplayer.chat"),
    SWAP("fakeplayer.swap"),

    SETTINGS("fakeplayer.settings"),
    SETTINGS_COLLIDABLE("fakeplayer.settings.collidable"),
    SETTINGS_PICKUP_ITEMS("fakeplayer.settings.pickupItems"),
    SETTINGS_INVULNERABLE("fakeplayer.settings.invulnerable"),
    SETTINGS_INFINITE_FOOD_LEVEL("fakeplayer.settings.infiniteFoodLevel"),
    SETTINGS_AUTO_REPLENISH("fakeplayer.settings.autoReplenish"),
    SETTINGS_AUTO_FISH("fakeplayer.settings.autoFish"),
    SETTINGS_SIMULATION_DISTANCE("fakeplayer.settings.simulationDistance"),
    SETTINGS_XP_NO_COOLDOWN("fakeplayer.settings.xpNoCooldown"),
    SETTINGS_AUTO_EQUIP_TOOL("fakeplayer.settings.autoEquipTool"),

    ACTION("fakeplayer.action"),
    ACTION_ATTACK("fakeplayer.action.attack"),
    ACTION_MINE("fakeplayer.action.mine"),
    ACTION_USE_ITEM("fakeplayer.action.use-item"),
    ACTION_DROP_ITEM("fakeplayer.action.drop-item"),
    ACTION_JUMP("fakeplayer.action.jump"),
    ACTION_SNEAK("fakeplayer.action.sneak"),

    OWNER_ADD("fakeplayer.owner.add"),
    OWNER_LIST("fakeplayer.owner.list"),
    OWNER_REMOVE("fakeplayer.owner.remove"),

}