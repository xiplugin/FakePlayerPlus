package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.action.*
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.component.FakePlayerDialog
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand

@Command("fakeplayer","fp")
class FakePlayerActionCommand: PluginComponent {

    @Subcommand("action")
    fun Player.actionUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionListDialog(
            fakePlayer,
            tl("fakeplayer.action.attack") to { attackActionUI(fakePlayer) },
            tl("fakeplayer.action.mine") to { mineActionUI(fakePlayer) },
            tl("fakeplayer.action.use-item") to { useItemActionUI(fakePlayer) },
            tl("fakeplayer.action.jump") to { jumpActionUI(fakePlayer) },
            tl("fakeplayer.action.sneak") to { sneakActionUI(fakePlayer) },
            tl("fakeplayer.gui.action.stop-all") to { stopAction(fakePlayer) }
        ))
    }

    @Subcommand("action stop")
    fun stopAction(@Select fakePlayer: FakePlayer) {
        fakePlayer.actions.stopAll()
    }

    @Subcommand("action attack")
    fun Player.attackActionUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionExecuteDialog(fakePlayer,
            onClickOnce = {
                fakePlayer.actions.dispatch(AttackOnce)
            },
            onClickInterval = { intervalTicks ->
                fakePlayer.actions.dispatch(AttackInterval(intervalTicks))
            },
            onClickStop = {
                fakePlayer.actions.stop(AttackAction.track)
            }
        ))
    }

    @Subcommand("action mine")
    fun Player.mineActionUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionExecuteDialog(fakePlayer,
            onClickContinuous = {
                fakePlayer.actions.dispatch(MineContinuous())
            },
            onClickStop = {
                fakePlayer.actions.stop(MineAction.track)
            }
        ))
    }

    @Subcommand("action use")
    fun Player.useItemActionUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionExecuteDialog(fakePlayer,
            onClickOnce = {
                fakePlayer.actions.dispatch(UseItemOnce)
            },
            onClickInterval = { intervalTicks ->
                fakePlayer.actions.dispatch(UseItemInterval(intervalTicks))
            },
            onClickContinuous = {
                fakePlayer.actions.dispatch(UseItemContinuous)
            },
            onClickStop = {
                fakePlayer.actions.stop(UseItemAction.track)
            }
        ))
    }

    @Subcommand("action jump")
    fun Player.jumpActionUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionExecuteDialog(fakePlayer,
            onClickOnce = {
                fakePlayer.actions.dispatch(JumpOnce)
            },
            onClickInterval = { intervalTicks ->
                fakePlayer.actions.dispatch(JumpInterval(intervalTicks))
            },
            onClickContinuous = {
                fakePlayer.actions.dispatch(JumpContinuous)
            },
            onClickStop = {
                fakePlayer.actions.stop(JumpAction.track)
            }
        ))
    }

    @Subcommand("action sneak")
    fun Player.sneakActionUI(@Select fakePlayer: FakePlayer) {
        showDialog(FakePlayerDialog.actionExecuteDialog(fakePlayer,
            onClickOnce = {
                fakePlayer.actions.dispatch(SneakOnce)
            },
            onClickContinuous = {
                fakePlayer.actions.dispatch(SneakContinuous)
            },
            onClickStop = {
                fakePlayer.actions.stop(SneakAction.track)
            }
        ))
    }

}