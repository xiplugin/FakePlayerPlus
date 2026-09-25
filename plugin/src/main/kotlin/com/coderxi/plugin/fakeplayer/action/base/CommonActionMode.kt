package com.coderxi.plugin.fakeplayer.action.base

enum class CommonActionMode(val key: String, val suggestParameters: Map<String, Any> = emptyMap()) {
    ONCE("once"),
    INTERVAL("interval", mapOf("intervalTicks" to 20)),
    CONTINUOUS("continuous");
    companion object {
        val ALL = setOf(ONCE, INTERVAL, CONTINUOUS)
    }
}