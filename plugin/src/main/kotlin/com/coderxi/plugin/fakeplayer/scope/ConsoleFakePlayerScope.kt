package com.coderxi.plugin.fakeplayer.scope

import com.coderxi.plugin.fakeplayer.entity.FakePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Server
import java.util.UUID

class ConsoleFakePlayerScope(val server: Server) : AbstractFakePlayerScope(UUID.nameUUIDFromBytes("${server.ip}:${server.port}".toByteArray())) {

    override fun notify(message: Component) = logger.info(MiniMessage.miniMessage().serialize(message))

    override fun checkSpawnLimit()  = true

    override fun getFakePlayerSpawnLocation() = server.worlds[0].spawnLocation

    override fun onFakePlayerSpawn(fakePlayer: FakePlayer) {
        fakePlayer.setPing((30..60).random())
    }

}