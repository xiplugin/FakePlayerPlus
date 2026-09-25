package com.coderxi.plugin.fakeplayer.command.permission

import org.bukkit.command.CommandSender

fun CommandSender.hasPermission(permission: Permission) = hasPermission(permission.value)