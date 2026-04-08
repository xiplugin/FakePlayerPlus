package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.command.Permission
import com.coderxi.plugin.fakeplayer.context.PluginContext
import org.bukkit.entity.Player

enum class Permission(override val node: String) : Permission {

    SPAWN("fakeplayer.spawn"),
    SPAWN_LIMIT_GROUP_TEMPLATE("fakeplayer.spawn.limit.{group}");

    companion object: PluginContext {
        val SPAWN_LIMIT_GROUPS: Map<Permission, Int> by lazy { config.spawnLimit.groups.map { (group, amount) -> SPAWN_LIMIT_GROUP_TEMPLATE.params("group" to group) to amount }.toMap() }
    }

}

fun Player.hasPermission(perm: Permission) = hasPermission(perm.node)