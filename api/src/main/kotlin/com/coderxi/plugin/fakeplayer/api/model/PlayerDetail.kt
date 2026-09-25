package com.coderxi.plugin.fakeplayer.api.model

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

data class PlayerDetail(
    val name: String,
    val uuid: UUID,
    val loginIp: String
) {
    companion object {
        private val ZERO_UUID = UUID(0L, 0L)
        fun of(player: Player): PlayerDetail {
            return PlayerDetail(
                name = player.name,
                uuid = player.uniqueId,
                loginIp = player.address?.address?.hostAddress ?: "127.0.0.1"
            )
        }
        fun of(uuid: UUID): PlayerDetail {
            if (uuid == ZERO_UUID) {
                return PlayerDetail(
                    name = Bukkit.getConsoleSender().name,
                    uuid = uuid,
                    loginIp = "127.0.0.1"
                )
            }
            val player = Bukkit.getPlayer(uuid)
            val offlineInfo = player ?: Bukkit.getOfflinePlayer(uuid)
            return PlayerDetail(
                name = offlineInfo.name ?: "UNKNOWN",
                uuid = uuid,
                loginIp = player?.address?.address?.hostAddress ?: "127.0.0.1"
            )
        }
    }
}