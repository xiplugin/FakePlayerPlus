package com.coderxi.plugin.fakeplayer.command.exception

import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler
import revxrsal.commands.bukkit.exception.SenderNotPlayerException
import com.coderxi.plugin.fakeplayer.command.exception.FakePlayerCommandException.*
import com.coderxi.plugin.fakeplayer.utils.messages.localOrDefault
import com.coderxi.plugin.fakeplayer.utils.messages.tlp
import revxrsal.commands.exception.CooldownException
import revxrsal.commands.exception.InvalidHelpPageException
import revxrsal.commands.exception.NoPermissionException
import revxrsal.commands.exception.UnknownCommandException
import revxrsal.commands.node.ExecutionContext
import java.util.concurrent.TimeUnit

import revxrsal.commands.exception.MissingArgumentException
import revxrsal.commands.node.ParameterNode

class FakePlayerCommandExceptionHandler : BukkitExceptionHandler() {

    typealias CommandContext = ExecutionContext<BukkitCommandActor>

    override fun onUnknownCommand(e: UnknownCommandException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp(actor.sender().localOrDefault(),"fakeplayer.command.unknown-command"))
    }

    override fun onNoPermission(e: NoPermissionException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp(actor.sender().localOrDefault(),"fakeplayer.command.no-permission"))
    }

    override fun onSenderNotPlayer(e: SenderNotPlayerException?, actor: BukkitCommandActor?) {
        actor?.sender()?.sendMessage(tlp(actor.sender().localOrDefault(),"fakeplayer.command.not-player"))
    }

    override fun onCooldown(e: CooldownException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp(actor.sender().localOrDefault(),"fakeplayer.command.cooldown", e.getTimeLeft(TimeUnit.SECONDS)))
    }

    override fun onInvalidHelpPage(e: InvalidHelpPageException, actor: BukkitCommandActor) {
        actor.sender().sendMessage(tlp(actor.sender().localOrDefault(),"fakeplayer.help.error.page-invalid", e.page(), e.numberOfPages()))
    }

    override fun onMissingArgument(e: MissingArgumentException, actor: BukkitCommandActor, parameter: ParameterNode<BukkitCommandActor, *>) {
        actor.sender().sendMessage(tlp(actor.sender().localOrDefault(),"fakeplayer.command.missing-argument", parameter.name()))
    }

    @HandleException
    fun handleCommandException(e: FakePlayerCommandException, actor: BukkitCommandActor) {
        val locale = actor.sender().localOrDefault()
        val message = when (e) {
            is FakePlayerCommandException.NoPermissionException -> tlp(locale,"fakeplayer.command.no-permission")
            is NotExitsException         -> tlp(locale,"fakeplayer.command.not-exists", e.name)
            is NotOwnerException         -> tlp(locale,"fakeplayer.command.not-owner", e.name)
            is NoSelectedException       -> tlp(locale,"fakeplayer.command.no-selected")
            is SpawnUnknownException     -> tlp(locale,"fakeplayer.spawn.failed")
            is SpawnServerLimitedException -> tlp(locale,"fakeplayer.spawn.failed.server-limited")
            is SpawnPlayerLimitedException -> tlp(locale,"fakeplayer.spawn.failed.player-limited")
            is SpawnIpLimitedException -> tlp(locale,"fakeplayer.spawn.failed.ip-limited")
            is SpawnAlreadyExistsException -> tlp(locale,"fakeplayer.spawn.failed.already-exists", e.name)
            is SpawnNameInvalidException -> tlp(locale,"fakeplayer.spawn.failed.name-invalid", e.name)
            is SpawnNameAlreadyUsedException -> tlp(locale,"fakeplayer.spawn.failed.name-already-used", e.name)
            is SpawnNoAvailableSequenceNameException -> tlp(locale,"fakeplayer.spawn.failed.no-available-sequence-name")
            is SpawnTpsAdaptiveLimitedException -> tlp(locale,"fakeplayer.spawn.failed.tps-adaptive-limited")
            is SpawnDisallowedException -> tlp(locale,"fakeplayer.spawn.failed.disallowed").append(e.causeMessage)
            is SpawnDuplicateSpawningException -> tlp(locale,"fakeplayer.spawn.failed.duplicate-spawning", e.name)
            is RenameNameInvalidException -> tlp(locale,"fakeplayer.rename.failed.name-invalid", e.name)
            is RenameAlreadyExistsException -> if (!e.hintForce) tlp(locale,"fakeplayer.rename.failed.already-exists", e.name) else tlp(locale,"fakeplayer.rename.failed.already-exists-hint-force", e.name)
            is UnsupportedActionModeException -> tlp(locale,"fakeplayer.command.unsupported-action-mode", e.name)
            is HasNoMoreExperience -> tlp(locale,"fakeplayer.expme.failed.has-no-experience",e.name)
            is OwnerMustBeHumanException -> tlp(locale,"fakeplayer.owner.add.failed.must-be-human", e.ownerName, e.fakePlayerName)
            is OwnerAlreadyBoundException -> tlp(locale,"fakeplayer.owner.add.failed.already-bound", e.ownerName, e.fakePlayerName)
            is OwnerIsCreatorCannotBeRemovedException -> tlp(locale,"fakeplayer.owner.remove.failed.is-creator", e.ownerName, e.fakePlayerName)
            is OwnerNotBoundCannotBeRemovedException -> tlp(locale,"fakeplayer.owner.remove.failed.not-bound", e.ownerName, e.fakePlayerName)
            is MissingDatabaseFileException -> tlp(locale,"fakeplayer.database.missing-file", e.name)
            is NoSuchTableException -> tlp(locale,"fakeplayer.database.no-such-table", e.name)
            else -> return e.printStackTrace()
        }
        actor.sender().sendMessage(message)
    }

}