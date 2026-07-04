package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerQuitedEvent
import com.coderxi.plugin.fakeplayer.utils.uniqueId
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FakePlayerSelector: Listener {

    private val selectedMap by lazy { ConcurrentHashMap<UUID, FakePlayer>() }

    private val CommandSender.selectedKey get() = uniqueId()

    var CommandSender.selected : FakePlayer?
        get() = selectedMap[selectedKey]
        set(value) {
            if(value == null) selectedMap.remove(selectedKey)
            else selectedMap[selectedKey] = value
        }

    @EventHandler
    private fun cleanup(event: FakePlayerQuitedEvent) {
        val uuid = event.fakePlayer.uuid
        selectedMap.entries.removeIf { it.value.uuid == uuid }
    }

}