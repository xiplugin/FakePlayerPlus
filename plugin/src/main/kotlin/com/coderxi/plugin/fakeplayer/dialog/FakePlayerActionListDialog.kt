package com.coderxi.plugin.fakeplayer.dialog

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission
import com.coderxi.plugin.fakeplayer.utils.FormDialog
import com.coderxi.plugin.fakeplayer.utils.hasPermission
import com.coderxi.plugin.fakeplayer.utils.plugin
import com.coderxi.plugin.fakeplayer.utils.tl
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
class FakePlayerActionListDialog(fakePlayer: FakePlayer, val viewer: Player) : FormDialog(
    tl("fakeplayer.gui.action.title", fakePlayer.name)
) {
    init {
        plugin.globalActionRegistry.actions.entries.filter { (actionType, _) ->
            val permissions = actionType.getAnnotation(PluginCommandPermission::class.java)
            permissions == null || viewer.hasPermission(permissions.node, permissions.or)
        }.forEach { (_, actionName) ->
            actionButton(
                ActionButton.create(
                    tl("fakeplayer.action.${actionName.lowercase().replace("_", "-")}"),
                    null,
                    100,
                    DialogAction.customClick({ _, _ ->
                        plugin.globalActionRegistry.getType(actionName)?.getConstructor()?.newInstance()
                            ?.let { action ->
                                FakePlayerActionExecuteDialog(fakePlayer, action, viewer).show(viewer)
                            }
                    }, defaultActionOptions)
                )
            )
        }
        actionButton(
            ActionButton.create(
                tl("fakeplayer.gui.action.stop-all"),
                null,
                100,
                DialogAction.customClick({ _, _ -> fakePlayer.actions.stopAll() }, defaultActionOptions)
            )
        )
        actionButtonColumns(1)
    }
}