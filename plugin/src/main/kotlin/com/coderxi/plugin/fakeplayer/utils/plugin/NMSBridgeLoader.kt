package com.coderxi.plugin.fakeplayer.utils.plugin

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl

object NMSBridgeLoader {

    // https://docs.papermc.io/paper/dev/internals/#getting-the-current-minecraft-version
    fun load(minecraftVersion: String): NMSBridge {
        val (mainVersion,v2,v3) = (0..2).map { minecraftVersion.split(".").getOrNull(it)?.toIntOrNull() ?: 0 }
        return when {
            minecraftVersion == "1.21.11" -> {
                NMSBridgeImpl()
            }
            mainVersion == 26 && v2 < 3 -> {
                com.coderxi.plugin.fakeplayer.nms.v26_1_1.NMSBridgeImpl()
            }
            mainVersion >= 26 -> {
                com.coderxi.plugin.fakeplayer.nms.v26_3.NMSBridgeImpl()
            }
            else -> {
                throw Exception("Running on an unsupported version ($minecraftVersion).")
            }
        }
    }

}