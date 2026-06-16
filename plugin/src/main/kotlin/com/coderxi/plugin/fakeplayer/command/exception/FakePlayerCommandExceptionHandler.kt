package com.coderxi.plugin.fakeplayer.command.exception

import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler
import revxrsal.commands.bukkit.exception.SenderNotPlayerException
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*

class FakePlayerCommandExceptionHandler : BukkitExceptionHandler(), PluginComponent  {

    @HandleException
    fun e(e: SenderNotPlayerException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.command.not-player"))
    }

    @HandleException
    fun e(e: NotExitsException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.command.not-exists",e.name))
    }

    @HandleException
    fun e(e: NotOwnerException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.command.not-owner",e.name))
    }

    @HandleException
    fun e(e: SpawnServerLimitedException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.spawn.failed.server-limited"))
    }
    @HandleException
    fun e(e: SpawnPlayerLimitedException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.spawn.failed.player-limited"))
    }

    @HandleException
    fun e(e: SpawnAlreadyExistsException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.spawn.failed.already-exists",e.name))
    }


}