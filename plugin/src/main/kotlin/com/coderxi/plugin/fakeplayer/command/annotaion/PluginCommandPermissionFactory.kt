package com.coderxi.plugin.fakeplayer.command.annotaion

import revxrsal.commands.Lamp
import revxrsal.commands.annotation.list.AnnotationList
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.command.CommandPermission

class PluginCommandPermissionFactory : CommandPermission.Factory<BukkitCommandActor> {
    override fun create(
        annotations: AnnotationList,
        lamp: Lamp<BukkitCommandActor?>
    ): CommandPermission<BukkitCommandActor?>? {
        val permissionAnno = annotations.get(PluginCommandPermission::class.java) ?: return null
        return CommandPermission { it.sender().hasPermission(permissionAnno.node.value)}
    }

}