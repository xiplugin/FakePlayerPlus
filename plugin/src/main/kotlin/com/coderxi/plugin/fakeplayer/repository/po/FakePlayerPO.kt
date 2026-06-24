package com.coderxi.plugin.fakeplayer.repository.po

import java.util.UUID

data class FakePlayerPO(
    val id: Int = 0,
    val name: String = "",
    val uuid: String = "",
    val creatorUuid: UUID? = null,
    val skin: String? = null,
    val settings: String? = null
)

