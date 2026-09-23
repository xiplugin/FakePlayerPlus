package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.utils.bukkit.uniqueIdOrZero
import com.coderxi.plugin.fakeplayer.utils.plugin.PluginComponent
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FakePlayerSelector: PluginComponent, Listener {

    private val selectedMap by lazy { ConcurrentHashMap<UUID, FakePlayer>() }

    var CommandSender.selected : FakePlayer?
        get() = selectedMap[uniqueIdOrZero]
        set(value) {
            if(value == null) selectedMap.remove(uniqueIdOrZero)
            else selectedMap[uniqueIdOrZero] = value
        }

    @EventHandler
    private fun cleanup(event: FakePlayerQuitedEvent) {
        val uuid = event.fakePlayer.uuid
        selectedMap.entries.removeIf { it.value.uuid == uuid }
    }

}