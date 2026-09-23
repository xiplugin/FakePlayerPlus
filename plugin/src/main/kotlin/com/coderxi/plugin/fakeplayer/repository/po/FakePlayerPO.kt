package com.coderxi.plugin.fakeplayer.repository.po

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import com.coderxi.plugin.fakeplayer.entity.StandardFakePlayer
import com.google.gson.Gson

import java.util.UUID

data class FakePlayerPO(
    val id: Int? = null,
    val name: String = "",
    val uuid: String = "",
    val creatorUuid: String? = null,
    val skin: String? = null,
    val settings: String? = null
) {

    fun toEntity(ownerUuids: Collection<UUID>) = StandardFakePlayer(
        name,
        UUID.fromString(uuid),
        creatorUuid?.runCatching { UUID.fromString(creatorUuid) }?.getOrNull(),
        ownerUuids,
        skin?.split("|")?.takeIf { it.size > 1 }?.let { PlayerTextures(it[0],it[1]) },
        (settings?.runCatching { gson.fromJson(settings, FakePlayerSettingsPO::class.java) }?.getOrNull() ?: FakePlayerSettingsPO()).toEntity()
    )

    companion object {

        private val gson = Gson()

        fun fromEntity(fakePlayer: FakePlayer) = FakePlayerPO(
            null,
            fakePlayer.name,
            fakePlayer.uuid.toString(),
            fakePlayer.creator?.uuid.toString(),
            if (fakePlayer.textures == null) null else "${fakePlayer.textures!!.value}|${fakePlayer.textures!!.signature}",
            gson.toJson(FakePlayerSettingsPO.fromEntity(fakePlayer.settings)),
        )
    }

}