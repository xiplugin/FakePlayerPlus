package com.coderxi.plugin.fakeplayer.action.type

import com.coderxi.plugin.fakeplayer.api.action.Action
import org.bukkit.block.Block
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission as Permission
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*

@Permission(ACTION_MINE, BASIC)
open class MineAction: Action {
    var target: Block? = null
    var progress = 0f
    var freezeTick = 0
}