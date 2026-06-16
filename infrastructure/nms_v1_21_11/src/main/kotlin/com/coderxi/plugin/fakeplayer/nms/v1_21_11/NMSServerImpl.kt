package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.mojang.authlib.GameProfile
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.storage.TagValueInput
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.craftbukkit.CraftServer
import java.util.UUID

class NMSServerImpl(override val server: Server) : NMSServer {

    val minecraftServer: MinecraftServer = (server as CraftServer).server

    override fun newPlayer(uuid: UUID, name: String): NMSServerPlayer {
        val serverPlayer = ServerPlayer(
            minecraftServer,
            NMSServerLevelImpl(Bukkit.getWorlds()[0]).handle,
            GameProfile(uuid, name),
            ClientInformation.createDefault()
        )
        minecraftServer.playerList.playerIo.load(serverPlayer.nameAndId()).ifPresent { nbt ->
            val valueInput = TagValueInput.create(
                ProblemReporter.DISCARDING,
                minecraftServer.registryAccess(),
                nbt
            )
            serverPlayer.load(valueInput)
        }
        return NMSServerPlayerImpl(serverPlayer.bukkitEntity)
    }
}