package com.coderxi.plugin.fakeplayer.action.type

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission as Permission
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*

@Permission(ACTION_JUMP)
class JumpAction: Action