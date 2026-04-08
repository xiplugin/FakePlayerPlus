package com.coderxi.plugin.fakeplayer.scope

import com.coderxi.plugin.fakeplayer.api.event.FakePlayerEvent
import com.coderxi.plugin.fakeplayer.command.Permission
import com.coderxi.plugin.fakeplayer.entity.FakePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.entity.Player

class PersonalFakePlayerScope(private val player: Player): AbstractFakePlayerScope(player.uniqueId) {

    override fun notify(message: Component) = player.sendMessage(message)

    override fun checkSpawnLimit(): Boolean =
        fakeplayers.size < (Permission.SPAWN_LIMIT_GROUPS.filter { (perm, _) -> player.hasPermission(perm.node) }.values.maxOrNull() ?: config.spawnLimit.default)

    override fun getFakePlayerSpawnLocation(): Location = player.location

    override fun onFakePlayerSpawn(fakePlayer: FakePlayer) {
        fakePlayer.setPing(-1)
        fakePlayer.player.apply {
            isPersistent = true
            isSleepingIgnored = true
            isInvulnerable = false
            isCollidable = true
            canPickupItems = true
            health = 20.0
            foodLevel = 20
        }
        val nametag = MiniMessage.miniMessage().deserialize("<aqua>[bot] <white>${fakePlayer.name}<br><red>❤❤❤❤❤❤❤❤❤❤")
        fakePlayer.showVirtualNameTag(player, nametag)
        fakePlayer.on<FakePlayerEvent.Death> {
            fakePlayer.hideVirtualNameTag(player)
        }
        fakePlayer.on<FakePlayerEvent.Respawn> {
            scheduler.runTaskLater(plugin, Runnable {
                fakePlayer.showVirtualNameTag(player, nametag)
            },1)
        }
    }

}