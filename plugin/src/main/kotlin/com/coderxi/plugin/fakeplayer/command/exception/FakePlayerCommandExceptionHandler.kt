package com.coderxi.plugin.fakeplayer.command.exception

import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler
import revxrsal.commands.bukkit.exception.SenderNotPlayerException
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.utils.tlp
import revxrsal.commands.exception.CooldownException
import revxrsal.commands.exception.NoPermissionException
import revxrsal.commands.exception.UnknownCommandException
import revxrsal.commands.node.ExecutionContext
import java.util.concurrent.TimeUnit

class FakePlayerCommandExceptionHandler : BukkitExceptionHandler() {

    typealias CommandContext = ExecutionContext<BukkitCommandActor>

    override fun onUnknownCommand(e: UnknownCommandException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.command.unknown-command"))
    }

    override fun onNoPermission(e: NoPermissionException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.command.no-permission"))
    }

    override fun onSenderNotPlayer(e: SenderNotPlayerException?, actor: BukkitCommandActor?) {
        actor?.sender()?.sendMessage(tlp("fakeplayer.command.not-player"))
    }

    override fun onCooldown(e: CooldownException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp("fakeplayer.command.cooldown", e.getTimeLeft(TimeUnit.SECONDS)))
    }

    @HandleException
    fun handleCommandException(e: FakePlayerCommandException, actor: BukkitCommandActor) {
        val message = when (e) {
            is FakePlayerCommandException.NoPermissionException -> tlp("fakeplayer.command.no-permission")
            is NotExitsException         -> tlp("fakeplayer.command.not-exists", e.name)
            is NotOwnerException         -> tlp("fakeplayer.command.not-owner", e.name)
            is NoSelectedException       -> tlp("fakeplayer.command.no-selected")
            is SpawnUnknownException     -> tlp("fakeplayer.spawn.failed")
            is SpawnServerLimitedException -> tlp("fakeplayer.spawn.failed.server-limited")
            is SpawnPlayerLimitedException -> tlp("fakeplayer.spawn.failed.player-limited")
            is SpawnIpLimitedException -> tlp("fakeplayer.spawn.failed.ip-limited")
            is SpawnAlreadyExistsException -> tlp("fakeplayer.spawn.failed.already-exists", e.name)
            is SpawnNameInvalidException -> tlp("fakeplayer.spawn.failed.name-invalid", e.name)
            is SpawnNameAlreadyUsedException -> tlp("fakeplayer.spawn.failed.name-already-used", e.name)
            is SpawnNoAvailableSequenceNameException -> tlp("fakeplayer.spawn.failed.no-available-sequence-name")
            is SpawnTpsAdaptiveLimitedException -> tlp("fakeplayer.spawn.failed.tps-adaptive-limited")
            is SpawnDisallowedException -> tlp("fakeplayer.spawn.failed.disallowed").append(e.causeMessage)
            is SpawnDuplicateSpawningException -> tlp("fakeplayer.spawn.failed.duplicate-spawning", e.name)
            is UnsupportedActionModeException -> tlp("fakeplayer.command.unsupported-action-mode", e.name)
            is HasNoMoreExperience -> tlp("fakeplayer.expme.failed.has-no-experience",e.name)
            is OwnerMustBeHumanException -> tlp("fakeplayer.owner.add.failed.must-be-human", e.ownerName, e.fakePlayerName)
            is OwnerAlreadyBoundException -> tlp("fakeplayer.owner.add.failed.already-bound", e.ownerName, e.fakePlayerName)
            is OwnerIsCreatorCannotBeRemovedException -> tlp("fakeplayer.owner.remove.failed.is-creator", e.ownerName, e.fakePlayerName)
            is OwnerNotBoundCannotBeRemovedException -> tlp("fakeplayer.owner.remove.failed.not-bound", e.ownerName, e.fakePlayerName)
            is MissingDatabaseFileException -> tlp("fakeplayer.database.missing-file", e.name)
            is NoSuchTableException -> tlp("fakeplayer.database.no-such-table", e.name)
            else -> return e.printStackTrace()
        }
        actor.sender().sendMessage(message)
    }

}