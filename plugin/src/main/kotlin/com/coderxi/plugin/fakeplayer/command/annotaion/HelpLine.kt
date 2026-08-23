package com.coderxi.plugin.fakeplayer.command.annotaion

import org.jetbrains.annotations.PropertyKey

annotation class HelpLine(
    @PropertyKey(resourceBundle = "messages.messages") val descriptionKey: String,
    val usage: String = "",
    val playerOnly: Boolean = false,
    vararg val children: HelpLine
)