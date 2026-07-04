package com.coderxi.plugin.fakeplayer.command

import com.coderxi.plugin.fakeplayer.api.action.*
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.annotaion.Select
import com.coderxi.plugin.fakeplayer.component.FakePlayerDialog
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission as Permission
import com.coderxi.plugin.fakeplayer.command.permission.Permission.*
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.tl
import net.kyori.adventure.text.Component

@Command("fakeplayer","fp")
class FakePlayerActionCommand {

    @Subcommand("action")
    @Permission(ACTION, BASIC)
    fun Player.actionUI(@Select fakePlayer: FakePlayer) {
        val textAndAction = mutableMapOf<Component,()-> Unit>()
        if (hasPermission(ACTION_ATTACK,BASIC)) textAndAction[tl("fakeplayer.action.attack")] = {attackActionUI(fakePlayer)}
        if (hasPermission(ACTION_MINE,BASIC)) textAndAction[tl("fakeplayer.action.mine")] = { mineActionUI(fakePlayer) }
        if (hasPermission(ACTION_USE_ITEM,BASIC)) textAndAction[tl("fakeplayer.action.use-item")] = {useItemActionUI(fakePlayer)}
        if (hasPermission(ACTION_JUMP,BASIC)) textAndAction[tl("fakeplayer.action.jump")] = {jumpActionUI(fakePlayer)}
        if (hasPermission(ACTION_SNEAK,BASIC)) textAndAction[tl("fakeplayer.action.sneak")] = {sneakActionUI(fakePlayer)}
        if (textAndAction.isNotEmpty()) textAndAction[tl("fakeplayer.gui.action.stop-all")] = {stopAction(fakePlayer)}
        showDialog(FakePlayerDialog.actionListDialog(fakePlayer,textAndAction))
    }

    @Subcommand("action stop")
    @Permission(ACTION, BASIC)
    fun stopAction(@Select fakePlayer: FakePlayer) {
        fakePlayer.actions.stopAll()
    }

    @Subcommand("action attack")
    @Permission(ACTION_ATTACK, BASIC)
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
    @Permission(ACTION_MINE, BASIC)
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
    @Permission(ACTION_USE_ITEM, BASIC)
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
    @Permission(ACTION_JUMP, BASIC)
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
    @Permission(ACTION_SNEAK, BASIC)
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