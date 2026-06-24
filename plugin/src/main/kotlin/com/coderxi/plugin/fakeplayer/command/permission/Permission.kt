package com.coderxi.plugin.fakeplayer.command.permission

enum class Permission(val value: String) {

    ADMIN("fakeplayer.admin"),
    BASIC("fakeplayer.basic"),

    RELOAD("fakeplayer.reload"),

    SPAWN("fakeplayer.spawn"),
    SPAWN_WITH_NAME("fakeplayer.spawn.name"),
    SPAWN_LIMIT_CUSTOM("fakeplayer.spawn.limit.{node}"),

    SELECT("fakeplayer.select"),

    REMOVE("fakeplayer.remove"),
    INVSEE("fakeplayer.invsee"),
    TP("fakeplayer.tp"),
    SKIN("fakeplayer.skin"),
    CMD("fakeplayer.command"),
    CHAT("fakeplayer.chat"),

    SETTINGS("fakeplayer.settings"),

    ACTION("fakeplayer.action"),
    ACTION_ATTACK("fakeplayer.action.attack"),
    ACTION_MINE("fakeplayer.action.mine"),
    ACTION_USE_ITEM("fakeplayer.action.use-item"),
    ACTION_JUMP("fakeplayer.action.jump"),
    ACTION_SNEAK("fakeplayer.action.sneak"),

    OWNER_ADD("fakeplayer.owner.add"),
    OWNER_REMOVE("fakeplayer.owner.remove"),

}