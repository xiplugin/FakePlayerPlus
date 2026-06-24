package com.coderxi.plugin.fakeplayer.command.exception

import revxrsal.commands.exception.CommandErrorException

abstract class FakePlayerCommandException : CommandErrorException() {

    class NotExitsException(val name: String) : FakePlayerCommandException()
    class NotOwnerException(val name: String) : FakePlayerCommandException()
    class NoSelectedException : FakePlayerCommandException()

    class SpawnUnknownException: FakePlayerCommandException()
    class SpawnServerLimitedException : FakePlayerCommandException()
    class SpawnPlayerLimitedException : FakePlayerCommandException()
    class SpawnIpLimitedException : FakePlayerCommandException()
    class SpawnTpsAdaptiveLimitedException : FakePlayerCommandException()
    class SpawnAlreadyExistsException(val name: String) : FakePlayerCommandException()
    class SpawnNameInvalidException(val name: String) : FakePlayerCommandException()
    class SpawnNameAlreadyUsedException(val name: String) : FakePlayerCommandException()
    class SpawnNoAvailableSequenceNameException : FakePlayerCommandException()

    class OwnerMustBeHumanException(val ownerName: String, val fakePlayerName: String) : FakePlayerCommandException()
    class OwnerAlreadyBoundException(val ownerName: String, val fakePlayerName: String) : FakePlayerCommandException()
    class OwnerIsCreatorCannotBeRemovedException(val ownerName: String, val fakePlayerName: String) : FakePlayerCommandException()
    class OwnerNotBoundCannotBeRemovedException(val ownerName: String, val fakePlayerName: String) : FakePlayerCommandException()

}