package com.coderxi.plugin.fakeplayer.command.exception

import revxrsal.commands.exception.CommandErrorException

abstract class FakePlayerCommandException : CommandErrorException() {

    class NotExitsException(val name: String) : FakePlayerCommandException()
    class NotOwnerException(val name: String) : FakePlayerCommandException()

    class SpawnServerLimitedException : FakePlayerCommandException()
    class SpawnPlayerLimitedException : FakePlayerCommandException()
    class SpawnAlreadyExistsException(val name: String) : FakePlayerCommandException()

}