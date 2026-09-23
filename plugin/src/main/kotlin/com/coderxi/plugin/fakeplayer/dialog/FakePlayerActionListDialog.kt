package com.coderxi.plugin.fakeplayer.dialog

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.command.annotaion.PluginCommandPermission
import com.coderxi.plugin.fakeplayer.command.permission.hasPermission
import com.coderxi.plugin.fakeplayer.plugin
import com.coderxi.plugin.fakeplayer.utils.bukkit.SimpleDialog
import com.coderxi.plugin.fakeplayer.utils.messages.tl
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
class FakePlayerActionListDialog(fakePlayer: FakePlayer, val viewer: Player) : SimpleDialog(
    tl(viewer,"fakeplayer.gui.action.title", fakePlayer.name), viewer
) {
    init {
        plugin.globalActionRegistry.actions.forEach { actionType ->
            val permissions = actionType.getAnnotation(PluginCommandPermission::class.java)
            if (permissions != null && !viewer.hasPermission(permissions.node)) {
                return@forEach
            }
            val actionName = plugin.globalActionRegistry.getName(actionType) ?: return@forEach
            val statusColor = Component.text("",if (fakePlayer.actions.activeActions.contains(actionType)) NamedTextColor.GREEN else NamedTextColor.WHITE)
            actionButton(
                ActionButton.create(
                    statusColor.append(tl(viewer,"fakeplayer.action.${actionName.replace("_","-")}")),
                    null,
                    100,
                    DialogAction.customClick({ _, _ ->
                        plugin.globalActionRegistry.getType(actionName)?.getConstructor()?.newInstance()
                            ?.let { action ->
                                FakePlayerActionExecuteDialog(fakePlayer, action, viewer).show(viewer, false)
                            }
                    }, defaultActionOptions)
                )
            )
        }
        actionButton(
            ActionButton.create(
                tl(viewer,"fakeplayer.gui.action.stop-all"),
                null,
                100,
                DialogAction.customClick({ _, _ -> fakePlayer.actions.stopAll() }, defaultActionOptions)
            )
        )
        actionButtonColumns(1)
    }
}