package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionMode
import com.coderxi.plugin.fakeplayer.api.action.ActionMode.*
import com.coderxi.plugin.fakeplayer.api.action.ActionType
import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.utils.ParamName
import com.coderxi.plugin.fakeplayer.utils.tl
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange
import io.papermc.paper.registry.data.dialog.input.DialogInput.bool as boolInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.dialog.DialogLike
import net.kyori.adventure.text.Component
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ACTION
import com.coderxi.plugin.fakeplayer.command.permission.Permission.BASIC
import com.coderxi.plugin.fakeplayer.utils.hasPermission

import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.command.CommandSender
import java.time.Duration

@Suppress("UnstableApiUsage")
object FakePlayerDialog {

    private val CANCEL_BTN by lazy { ActionButton.create(tl("fakeplayer.gui.cancel"),null, 100, null) }
    private val ACTION_OPTIONS by lazy { ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build() }
    private val EMPTY_TEXT by lazy { DialogBody.plainMessage(Component.text(" ")) }

    fun settingsDialog(fakePlayer: FakePlayer, onSubmit: () -> Unit = {}): DialogLike {
        val settings = fakePlayer.settings
        val inputs = listOf(
            boolInput("collidable", tl("fakeplayer.gui.settings.collidable")).initial(settings.collidable).build(),
            boolInput("pickupItems", tl("fakeplayer.gui.settings.pickup-items")).initial(settings.pickupItems).build(),
            boolInput("invulnerable", tl("fakeplayer.gui.settings.invulnerable")).initial(settings.invulnerable).build(),
            boolInput("autoReplenish", tl("fakeplayer.gui.settings.auto-replenish")).initial(settings.autoReplenish).build(),
            boolInput("autoFish", tl("fakeplayer.gui.settings.auto-fish")).initial(settings.autoFish).build(),
        )
        val onSubmitClick = DialogAction.customClick(
            { view, _ ->
                fakePlayer.settings = FakePlayerSettings(
                    view.getBoolean("collidable") ?: settings.collidable,
                    view.getBoolean("pickupItems") ?: settings.pickupItems,
                    view.getBoolean("invulnerable") ?: settings.invulnerable,
                    view.getBoolean("autoReplenish") ?: settings.autoReplenish,
                    view.getBoolean("autoFish") ?: settings.autoFish,
                )
                onSubmit.invoke()
            },
            ACTION_OPTIONS
        )
        return Dialog.create { builder -> builder.empty()
            .base(DialogBase.builder(tl("fakeplayer.gui.settings.title",fakePlayer.name))
                .canCloseWithEscape(true)
                .inputs(inputs)
                .build())
            .type(DialogType.confirmation(
                ActionButton.create(tl("fakeplayer.gui.submit"),null, 100, onSubmitClick),
                CANCEL_BTN
            ))
        }
    }

    fun actionExecuteDialog(fakePlayer: FakePlayer, actionType: ActionType): DialogLike {
        val actionButtons = mutableListOf<ActionButton>()
        var columns = 0
        val modes = Action.getSupportModes(actionType)
        modes.forEach { modeClass ->
           actionButtons.add(ActionButton.create(
               tl("fakeplayer.gui.action.execute-${modeClass.simpleName.lowercase()}"), null, 100,
               DialogAction.customClick({ view, _ ->
                   val modeConstructor = modeClass.declaredConstructors.first()
                   val modeInstance = try {
                       val instanceField = modeClass.getDeclaredField("INSTANCE")
                       instanceField.isAccessible = true
                       instanceField.get(null) as ActionMode
                   } catch (e: NoSuchFieldException) {
                       modeConstructor.newInstance(*modeConstructor.parameters.map { param ->
                           val name = param.getAnnotation(ParamName::class.java).value
                           when (param.type) {
                               Int::class.java, Int::class.javaPrimitiveType -> view.getFloat(name)?.toInt()
                               Double::class.java, Double::class.javaPrimitiveType -> view.getFloat(name)?.toDouble()
                               Float::class.java, Float::class.javaPrimitiveType -> view.getFloat(name)
                               Boolean::class.java, Boolean::class.javaPrimitiveType -> view.getBoolean(name)
                               String::class.java -> view.getText(name)
                               else -> null
                           }
                       }.toTypedArray<Any?>())
                   }
                   val action = Action.toClass(actionType).getConstructor(modeClass).newInstance(modeInstance)
                   fakePlayer.actions.dispatch(action) }, ACTION_OPTIONS)))
            columns++
        }
        actionButtons.add(ActionButton.create(
            tl("fakeplayer.gui.action.stop"), null, 100,
            DialogAction.customClick({ _, _ -> fakePlayer.actions.stop(actionType.track) }, ACTION_OPTIONS)
        ))
        val inputs = mutableListOf<DialogInput>()
        if (modes.contains(Interval::class.java)) {
            inputs.add(numberRange("intervalTicks", tl("fakeplayer.gui.action.interval-ticks"), 1f, 200f).step(1f).initial(20f).width(100).build())
        }
        return Dialog.create { builder -> builder.empty()
            .base(DialogBase.builder(tl("fakeplayer.gui.action.title", fakePlayer.name))
                .canCloseWithEscape(true)
                .body(if (inputs.isNotEmpty()) listOf(EMPTY_TEXT) else listOf(EMPTY_TEXT,EMPTY_TEXT))
                .inputs(inputs)
                .build())
            .type(DialogType.multiAction(actionButtons).columns(columns).exitAction(CANCEL_BTN).build())
        }
    }

    fun actionListDialog(viewer: CommandSender, fakePlayer: FakePlayer): DialogLike {
        val actionTypes = if (viewer.hasPermission(BASIC)) ActionType.entries else ActionType.entries.filter { viewer.hasPermission("${ACTION.value}.${it.name.lowercase()}") }
        val textAndAction = actionTypes
            .associateTo((mutableMapOf())) { type ->
                tl("fakeplayer.action.${type.name.lowercase().replace("_","-")}") to
                        { viewer.showDialog(FakePlayerDialog.actionExecuteDialog(fakePlayer, type)) }
            }
        if (textAndAction.isNotEmpty()) textAndAction[tl("fakeplayer.gui.action.stop-all")] = {fakePlayer.actions.stopAll()}
        val actionButtons = textAndAction.map { (text, action) ->
            ActionButton.create(text, null, 100, DialogAction.customClick({ _, _ -> action() }, ACTION_OPTIONS) )
        }
        return Dialog.create { builder ->  builder.empty()
           .base(DialogBase.builder(tl("fakeplayer.gui.action.title", fakePlayer.name)).canCloseWithEscape(true).build())
           .type(DialogType.multiAction(actionButtons).columns(1).exitAction(CANCEL_BTN).build())
        }
    }

}