package com.coderxi.plugin.fakeplayer.manager

import com.coderxi.plugin.fakeplayer.api.event.FakePlayerEvent
import com.coderxi.plugin.fakeplayer.api.event.FakePlayerEvent.*
import com.coderxi.plugin.fakeplayer.context.PluginContext
import com.coderxi.plugin.fakeplayer.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.MinecraftSpritesUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FakePlayerNametagManager: PluginContext {

    val registry = FakePlayerRegistry
    lateinit var nametagTemplate: Component
    val nametagTargetsGetters = ConcurrentHashMap<UUID, () -> Collection<Player>>()
    val nametagArgsSetters: MutableMap<String, (FakePlayer) -> Component> = mutableMapOf(
        "{fakeplayer_name}" to { fp -> Component.text(fp.name) },
        "{fakeplayer_health_sprites}" to { fp -> MinecraftSpritesUtil.health2heartsSprites(fp.player) },
        "{fakeplayer_health}" to { fp -> Component.text("%.1f".format(fp.player.health))},
        "{fakeplayer_level}" to { fp -> Component.text(fp.player.level) },
        "{fakeplayer_expProgress}" to { fp -> Component.text("${(fp.player.exp * 100).toInt()}%")},
        "{fakeplayer_expToLevel}" to { fp -> Component.text("${(fp.player.exp * fp.player.expToLevel).toInt()}/${fp.player.expToLevel}")},
        "{fakeplayer_status_sprites}" to { Component.empty() } //TODO
    )
    lateinit var refreshEvents: List<Class<out FakePlayerEvent>>
    var refreshTask: BukkitTask? = null
    private val updatingFakePlayers = ConcurrentHashMap.newKeySet<UUID>()

    init {
        onPluginEnable { init() }
        onPluginReload { refreshTask?.cancel(); init() }
        onPluginDisable { refreshTask?.cancel() }
    }

    fun init() {
        nametagTemplate = MiniMessage.miniMessage().deserialize(config.nametag.lines.joinToString("<br><reset>"))
        refreshEvents = config.nametag.refreshEvents.mapNotNull { name ->
            runCatching { Class.forName("com.coderxi.plugin.fakeplayer.events.FakePlayerEvent.$$name") }.getOrNull() as Class<out FakePlayerEvent>?
        }
        println(refreshEvents)
        refreshTask = scheduler.runTaskTimerAsynchronously(plugin, Runnable {
            //排空假人队列到局部变量，防止并发修改异常
            val updatingFakePlayersClone = mutableSetOf<UUID>().also { clone ->
                val iterator = updatingFakePlayers.iterator()
                while (iterator.hasNext()) {
                    clone.add(iterator.next())
                    iterator.remove()
                }
            }
            //处理每一个需要更新的假人
            if (updatingFakePlayersClone.isEmpty()) return@Runnable
            updatingFakePlayersClone.forEach { fakePlayerUUID ->
                val fakePlayer = registry.getFakePlayer(fakePlayerUUID) ?: return@forEach
                val nametag = fillTemplate(fakePlayer)
                val targets = nametagTargetsGetters[fakePlayerUUID]?.invoke() ?: return@forEach
                targets.forEach { target -> fakePlayer.updateVirtualNametag(target, nametag) }
            }
        }, 0L, config.nametag.refreshIntervalTick)
    }

    private fun fillTemplate(fakePlayer: FakePlayer) = nametagArgsSetters.entries.fold(nametagTemplate) { nametag, (key, setter) ->
        nametag.replaceText { it.matchLiteral(key).replacement(setter(fakePlayer)) }
    }

    fun showNametag(fakePlayer: FakePlayer, nametag: Component = fillTemplate(fakePlayer)) {
        nametagTargetsGetters[fakePlayer.uniqueId]?.invoke()?.forEach { fakePlayer.showVirtualNametag(it, nametag) }
    }

    fun updateNametag(fakePlayer: FakePlayer) {
        updatingFakePlayers.add(fakePlayer.uniqueId)
    }

    fun hideNametag(fakePlayer: FakePlayer) {
        nametagTargetsGetters[fakePlayer.uniqueId]?.invoke()?.forEach { fakePlayer.hideVirtualNametag(it) }
    }

    fun bind(fakePlayer: FakePlayer, targetsGetter: () -> Collection<Player>) {
        nametagTargetsGetters[fakePlayer.uniqueId] = targetsGetter
        showNametag(fakePlayer)
        fakePlayer.apply {
            on<Death> {
                schedulerRunLaterAsync(20) { hideNametag(this) }
            }
            on<Respawn> {
                schedulerRunLaterAsync { showNametag(this) }
            }
            config.nametag.refreshEvents.forEach { e ->  on(e) { updateNametag(this) } }
            on<Quit> {
                hideNametag(this)
                nametagTargetsGetters.remove(uniqueId)
            }
        }
    }

}