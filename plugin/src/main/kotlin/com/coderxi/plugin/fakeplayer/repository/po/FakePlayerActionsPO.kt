package com.coderxi.plugin.fakeplayer.repository.po

import com.coderxi.plugin.fakeplayer.api.model.ActionState

data class FakePlayerActionsPO(
    val actives : Collection<ActionState>? = null
)