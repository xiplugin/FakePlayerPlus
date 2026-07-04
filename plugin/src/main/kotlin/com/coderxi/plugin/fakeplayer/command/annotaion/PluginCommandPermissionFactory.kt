package com.coderxi.plugin.fakeplayer.command.annotaion

import com.coderxi.plugin.fakeplayer.utils.hasPermission
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
        return CommandPermission { actor ->
            val sender = actor.sender()
            sender.hasPermission(permissionAnno.node,permissionAnno.or)
        }
    }

}