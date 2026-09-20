package com.coderxi.plugin.fakeplayer.dialog

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ADMIN
import com.coderxi.plugin.fakeplayer.utils.FormDialog
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.launch
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.tl
import com.coderxi.plugin.fakeplayer.utils.tlp
import com.coderxi.plugin.fakeplayer.utils.tls
import org.bukkit.entity.Player

class FakePlayerSettingsDialog(fakePlayer: FakePlayer, val viewer: Player): FormDialog(
    tl("fakeplayer.gui.settings.title",fakePlayer.name)
) {

    init {
        boolSingleOption(fakePlayer::collidable, tl("fakeplayer.gui.settings.collidable"))
        boolSingleOption(fakePlayer::pickupItems, tl("fakeplayer.gui.settings.pickup-items"))
        boolSingleOption(fakePlayer::invulnerable, tl("fakeplayer.gui.settings.invulnerable"))
        boolSingleOption(fakePlayer::autoReplenish, tl("fakeplayer.gui.settings.auto-replenish"))
        boolSingleOption(fakePlayer::autoFish, tl("fakeplayer.gui.settings.auto-fish"))
        numberRange(fakePlayer::simulationDistance, tl("fakeplayer.gui.settings.simulation-distance"), "%s: %s"+tls("fakeplayer.gui.unit.chunk") ,
            start = 1,
            end = if (viewer.hasPermission(ADMIN)) 32 else plugin.server.simulationDistance
        )
        boolSingleOption(fakePlayer::xpNoCooldown, tl("fakeplayer.gui.settings.xp-no-cooldown"))
        boolSingleOption(fakePlayer::autoEquipTool, tl("fakeplayer.gui.settings.auto-equip-tool"))
        submitButton {
            viewer.sendMessage(tlp("fakeplayer.gui.settings.submit.success", fakePlayer.name))
            launch { plugin.fakePlayerManager.saveSettings(fakePlayer) }
        }
    }

}