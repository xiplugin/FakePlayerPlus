package com.coderxi.plugin.fakeplayer.dialog

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.FormDialog
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.tl
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
class FakePlayerActionExecuteDialog (fakePlayer: FakePlayer, action: Action, val viewer: Player): FormDialog(
    tl("fakeplayer.gui.action.title",fakePlayer.name)
) {
    init {
        val modes = plugin.globalActionRegistry.getModes(action.javaClass)
        modes?.forEach { mode ->
            val suggestParameters = plugin.globalActionRegistry.getModeSuggestParameters(mode)
            suggestParameters.entries.forEach { (key, value) ->
                if (value is Int) {
                    numberRange(
                        key,
                        value,
                        tl("fakeplayer.gui.action.params.$key"),
                        null,
                        1,
                        200,
                    )
                } else {
                    text(
                        key,
                        value.toString(),
                        tl("fakeplayer.gui.action.params.$key")
                    )
                }
            }

            actionButton(
                ActionButton.create(
                    tl("fakeplayer.gui.action.execute-$mode"),
                    null,
                    100,
                    DialogAction.customClick({ view, _ ->
                        val params = suggestParameters.entries.associate { (key, value) ->
                            if (value is Int) {
                                key to ((view.getFloat(key) ?: value).toInt().toString())
                            } else {
                                key to (view.getText(key) ?: value.toString())
                            }
                        }
                        println(params)
                        fakePlayer.actions.execute(action, mode, params)
                    }, defaultActionOptions)
                )
            )
        }

        if (fakePlayer.actions.activeActions.contains(action.javaClass)) {
            actionButton(
                ActionButton.create(
                    tl("fakeplayer.gui.action.stop"),
                    null,
                    100,
                    DialogAction.customClick({ _, _ -> fakePlayer.actions.stop(action) }, defaultActionOptions)
                )
            )
        }

        actionButtonColumns(modes?.size?.coerceAtLeast(1) ?: 1)
    }
}