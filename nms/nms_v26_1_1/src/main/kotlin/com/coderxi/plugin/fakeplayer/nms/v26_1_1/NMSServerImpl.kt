package com.coderxi.plugin.fakeplayer.nms.v26_1_1

import com.coderxi.plugin.fakeplayer.api.nms.NMSServerGamePacketListener
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.network.FakeConnection
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.Server
import java.util.UUID

open class NMSServerImpl(server: Server) : com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSServerImpl(server) {

    @Suppress("UNCHECKED_CAST")
    override fun <T> newGamePacketListener(
        server: DedicatedServer,
        connection: FakeConnection,
        handle: ServerPlayer,
        cookie: CommonListenerCookie
    ): T where T : ServerGamePacketListenerImpl, T : NMSServerGamePacketListener {
        return NMSServerGamePacketListenerImpl(server,connection,handle,cookie) as T
    }

    override fun migratePlayerData(oldUuid: UUID, newUuid: UUID) {
        if (oldUuid == newUuid) return
        val worldPlayersPath = server.worldContainer.toPath().resolve("world/players")
        migratePlayerDataFile(worldPlayersPath.resolve("advancements"), oldUuid, newUuid, ".json")
        migratePlayerDataFile(worldPlayersPath.resolve("data"), oldUuid, newUuid, ".dat")
        migratePlayerDataFile(worldPlayersPath.resolve("data"), oldUuid, newUuid, ".dat_old")
        migratePlayerDataFile(worldPlayersPath.resolve("stats"), oldUuid, newUuid, ".json")
    }
}