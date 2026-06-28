package com.coderxi.plugin.fakeplayer.utils

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge

object NMSBridgeLoader : PluginComponent {

    // https://docs.papermc.io/paper/dev/internals/#getting-the-current-minecraft-version
    fun load(minecraftVersion: String): NMSBridge {
        val (mainVersion,v2) = (0..2).map { minecraftVersion.split(".").getOrNull(it)?.toIntOrNull() ?: 0 }
        return when {
            minecraftVersion == "1.21.11" -> {
                com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl()
            }
            mainVersion == 26 && v2 == 1 -> {
                com.coderxi.plugin.fakeplayer.nms.v26_1_1.NMSBridgeImpl()
            }
            mainVersion >= 26 -> {
                com.coderxi.plugin.fakeplayer.nms.v26_2.NMSBridgeImpl()
            }
            else -> {
                throw Exception("Running on an unsupported version ($minecraftVersion).")
            }
        }
    }

}