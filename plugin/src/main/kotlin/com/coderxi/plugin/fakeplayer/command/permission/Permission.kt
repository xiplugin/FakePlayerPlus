package com.coderxi.plugin.fakeplayer.command.permission

import org.bukkit.permissions.Permission as Permission0

enum class Permission(val value: String) {

    ADMIN("fakeplayer.admin"),

    HELP("fakeplayer.help"),
    RELOAD("fakeplayer.reload"),

    SPAWN("fakeplayer.spawn"),
    SPAWN_WITH_NAME("fakeplayer.spawn.name"),
    SPAWN_LIMIT_CUSTOM("fakeplayer.spawn.limit.{node}"),

    SELECT("fakeplayer.select"),

    REMOVE("fakeplayer.remove"),

    KILL("fakeplayer.kill"),
    RESPAWN("fakeplayer.respawn"),
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
    SETTINGS_KEEP_INVENTORY("fakeplayer.settings.keepInventory"),
    SETTINGS_INTERACTED_ACTION("fakeplayer.settings.interactedAction"),
    SETTINGS_SHIFT_INTERACTED_ACTION("fakeplayer.settings.shiftInteractedAction"),
    SETTINGS_DEATH_ACTION("fakeplayer.settings.deathAction"),
    SETTINGS_FOLLOW_QUITING("fakeplayer.settings.followQuiting"),
    SETTINGS_FOLLOW_QUITING_DELAY("fakeplayer.settings.followQuitingDelay"),

    ACTION("fakeplayer.action"),
    ACTION_ATTACK("fakeplayer.action.attack"),
    ACTION_MINE("fakeplayer.action.mine"),
    ACTION_USE_ITEM("fakeplayer.action.use-item"),
    ACTION_DROP_ITEM("fakeplayer.action.drop-item"),
    ACTION_JUMP("fakeplayer.action.jump"),
    ACTION_SNEAK("fakeplayer.action.sneak"),
    ACTION_LOOT_AT_ENTITY("fakeplayer.action.look-at-entity"),

    OWNER_ADD("fakeplayer.owner.add"),
    OWNER_LIST("fakeplayer.owner.list"),
    OWNER_REMOVE("fakeplayer.owner.remove"),

    ;
    companion object {
        val BASIC = Permission0("fakeplayer.basic", "Basic FakePlayer permissions", mapOf(
            HELP.value to true,
            SPAWN.value to true,
            SELECT.value to true,
            REMOVE.value to true,
            KILL.value to true,
            RESPAWN.value to true,
            INVSEE.value to true,
            ENDER_CHEST.value to true,
            TP.value to true,
            EXPME.value to true,
            SKIN.value to true,
            CMD.value to true,
            CHAT.value to true,
            SWAP.value to true,
            SETTINGS.value to true,
            SETTINGS_COLLIDABLE.value to true,
            SETTINGS_PICKUP_ITEMS.value to true,
            SETTINGS_INVULNERABLE.value to true,
            SETTINGS_INFINITE_FOOD_LEVEL.value to true,
            SETTINGS_AUTO_REPLENISH.value to true,
            SETTINGS_AUTO_FISH.value to true,
            SETTINGS_SIMULATION_DISTANCE.value to true,
            SETTINGS_XP_NO_COOLDOWN.value to true,
            SETTINGS_AUTO_EQUIP_TOOL.value to true,
            SETTINGS_KEEP_INVENTORY.value to true,
            ACTION.value to true,
            ACTION_ATTACK.value to true,
            ACTION_MINE.value to true,
            ACTION_USE_ITEM.value to true,
            ACTION_DROP_ITEM.value to true,
            ACTION_JUMP.value to true,
            ACTION_SNEAK.value to true,
            ACTION_LOOT_AT_ENTITY.value to true,
            OWNER_ADD.value to true,
            OWNER_LIST.value to true,
            OWNER_REMOVE.value to true,
        ))
    }

}