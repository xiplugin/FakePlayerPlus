package com.coderxi.plugin.fakeplayer

import com.coderxi.plugin.fakeplayer.api.nms.NMSBridge
import com.coderxi.plugin.fakeplayer.config.PluginConfig
import com.coderxi.plugin.fakeplayer.context.PluginContext
import com.coderxi.plugin.fakeplayer.event.FakePlayerEventListener
import com.coderxi.plugin.fakeplayer.manager.FakePlayerRegistry
import com.coderxi.plugin.fakeplayer.manager.PluginConfigManager
import com.coderxi.plugin.fakeplayer.nms.v1_21_11.NMSBridgeImpl
import com.coderxi.plugin.fakeplayer.scope.PersonalFakePlayerScope
import com.coderxi.plugin.fakeplayer.utils.MessagesUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

class FakePlayerPlusPlugin : JavaPlugin() {

    lateinit var bridge: NMSBridge private set
    lateinit var config : PluginConfig private set
    lateinit var messages : MessagesUtil private set
    val listeners = listOf(
        FakePlayerEventListener()
    )

    override fun onEnable() {
        bridge = NMSBridgeImpl()
        messages = MessagesUtil()
        config = PluginConfigManager.load<PluginConfig>("config.yml")
        messages.updateLocale(config.language)
        PluginContext.emit("Enable")
    }

    fun onReload() {
        config.load()
        messages.updateLocale(config.language)
        PluginContext.emit("Reload")
    }

    override fun onDisable() {
        listeners.forEach { HandlerList.unregisterAll(it) }
        PluginContext.emit("Disable")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (args[0] == "reload") {
            onReload()
            return true
        }
        val scope = FakePlayerRegistry.getScope((sender as Player).uniqueId) ?: PersonalFakePlayerScope(sender)
        when (args[0]) {
            "spawn" -> scope.spawnAsync(args[1]).exceptionally { e ->
                e.printStackTrace()
                null
            }
        }
        return true
    }

}