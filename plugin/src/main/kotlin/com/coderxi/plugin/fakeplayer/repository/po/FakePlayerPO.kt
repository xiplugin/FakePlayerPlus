package com.coderxi.plugin.fakeplayer.repository.po

data class FakePlayerPO(
    val id: Int = 0,
    val name: String = "",
    val uuid: String = "",
    val creatorUuid: String? = null,
    val skin: String? = null,
    val settings: String? = null
)

