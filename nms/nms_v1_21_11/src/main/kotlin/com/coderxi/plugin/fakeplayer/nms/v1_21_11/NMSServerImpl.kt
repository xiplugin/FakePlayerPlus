package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi.Companion.api
import com.coderxi.plugin.fakeplayer.api.nms.NMSServer
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.api.nms.NMSServerPlayer
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.network.FakeConnection
import com.mojang.authlib.GameProfile
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import java.io.IOException
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.io.path.notExists
import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi.Companion.javaPlugin as plugin

open class NMSServerImpl(override val server: Server) : NMSServer {

    override fun newPlayer(uuid: UUID, name: String, location: Location): NMSServerPlayer {
        val serverHandle = (server as CraftServer).handle
        val playerHandle = ServerPlayer(
            serverHandle.server,
            (api.nms.fromWorld(location.world).world as CraftWorld).handle,
            GameProfile(uuid, name),
            ClientInformation.createDefault()
        )
        playerHandle.bukkitEntity.loadData()
        playerHandle.absSnapTo(location.x,location.y,location.z,location.yaw,location.pitch)
        return api.nms.fromPlayer(playerHandle.bukkitEntity)
    }

    override fun placeNewPlayer(player: Player, address: InetAddress): NMSServerGamePacketListener {
        val serverHandle = (server as CraftServer).handle
        val playerHandle = (player as CraftPlayer).handle
        val connection = FakeConnection(address)
        val cookie = CommonListenerCookie.createInitial(playerHandle.gameProfile, false)
        serverHandle.placeNewPlayer(connection, playerHandle, cookie)
        val packetListener = newGamePacketListener(serverHandle.server, connection, playerHandle, cookie) as ServerGamePacketListenerImpl
        playerHandle.connection = packetListener
        connection.packetListenerImpl = packetListener
        return packetListener as NMSServerGamePacketListener
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> newGamePacketListener(
        server: DedicatedServer,
        connection: FakeConnection,
        handle: ServerPlayer,
        cookie: CommonListenerCookie
    ): T where T : ServerGamePacketListenerImpl, T : NMSServerGamePacketListener {
        return NMSServerGamePacketListenerImpl(server, connection, handle, cookie) as T
    }

    override fun migratePlayerData(oldUuid: UUID, newUuid: UUID) {
        val worldPath = server.worldContainer.toPath().resolve("world")
        migratePlayerDataFile(worldPath.resolve("advancements"), oldUuid, newUuid, ".json")
        migratePlayerDataFile(worldPath.resolve("playerdata"), oldUuid, newUuid, ".dat")
        migratePlayerDataFile(worldPath.resolve("playerdata"), oldUuid, newUuid, ".dat_old")
        migratePlayerDataFile(worldPath.resolve("stats"), oldUuid, newUuid, ".json")
    }

    fun migratePlayerDataFile(folder: Path, oldUuid: UUID, newUuid: UUID, suffix: String) {
        val oldPath = folder.resolve("$oldUuid$suffix")
        if (oldPath.notExists()) return
        val newPath = folder.resolve("$newUuid$suffix")
        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            plugin.logger.warning { "Failed to migrate player data file $oldUuid to $newUuid" }
        }
    }

}
