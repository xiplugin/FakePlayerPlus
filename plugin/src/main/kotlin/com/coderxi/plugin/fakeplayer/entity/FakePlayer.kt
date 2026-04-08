package com.coderxi.plugin.fakeplayer.entity

import com.coderxi.plugin.fakeplayer.api.event.FakePlayerEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerEvent.*
import com.coderxi.plugin.fakeplayer.api.nms.*
import com.coderxi.plugin.fakeplayer.config.OnDeathAction
import com.coderxi.plugin.fakeplayer.context.PluginContext
import com.coderxi.plugin.fakeplayer.utils.EventBus
import com.coderxi.plugin.fakeplayer.utils.EventDispatcher
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class FakePlayer(private val handle: NMSServerPlayer) : EventDispatcher<FakePlayerEvent>, PluginContext {

    val player = handle.getPlayer()
    val uniqueId = player.uniqueId
    lateinit var connection: NMSServerGamePacketListener
    override val eventBus: EventBus = EventBus()

    val name get() = player.name

    fun setPing(ping: Int) {
        connection.setPing(ping)
    }

    fun chat(message: String) {
        player.chat(message)
    }

    fun quit(cause: String = "quit") {
        player.kick(Component.text(cause))
    }

    fun doTick() {
        handle.doTick()
    }

    fun requestRespawn() {
        handle.requestRespawn()
    }

    fun teleportAsync(location: Location): CompletableFuture<Boolean?> {
        return player.teleportAsync(location)
    }

    fun showVirtualNameTag(player: Player, content: Component) {
        handle.showVirtualNameTag(player,content)
    }

    fun hideVirtualNameTag(player: Player) {
        handle.hideVirtualNameTag(player)
    }

    private var respawnBackLocation: Location? = null

    init {
        on<Death> { event ->
            when (config.onDeathAction) {
                OnDeathAction.NONE -> null
                OnDeathAction.QUIT -> Runnable { quit("You died") }
                OnDeathAction.RESPAWN -> Runnable { requestRespawn() }
                OnDeathAction.RESPAWN_BACK ->  Runnable {
                    respawnBackLocation = event.location
                    requestRespawn()
                }
            }?.also { scheduler.runTaskLater(plugin, it, 20) }
        }
        on<Respawn> {
            respawnBackLocation?.let { loc ->
                respawnBackLocation = null
                player.teleportAsync(loc)
            }
        }
    }


}