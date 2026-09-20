package com.coderxi.plugin.fakeplayer.command.parameter

data class ActionModeAndParameters(
    val mode: String,
    val parameters: Map<String, String>
)