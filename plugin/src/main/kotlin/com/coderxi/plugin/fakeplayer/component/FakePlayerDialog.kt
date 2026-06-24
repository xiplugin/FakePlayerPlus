package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.utils.PluginComponent
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

import net.kyori.adventure.text.event.ClickCallback
import java.time.Duration

@Suppress("UnstableApiUsage")
object FakePlayerDialog: PluginComponent {

    private val CANCEL_BTN by lazy { ActionButton.create(tl("fakeplayer.gui.cancel"),null, 100, null) }
    private val ACTION_OPTIONS by lazy { ClickCallback.Options.builder().uses(1).lifetime(Duration.ofHours(1)).build() }
    private val EMPTY_TEXT by lazy { DialogBody.plainMessage(Component.text(" ")) }

    fun settingsDialog(fakePlayer: FakePlayer, onSubmit: () -> Unit = {}): DialogLike {
        val settings = fakePlayer.settings
        val inputs = listOf(
            boolInput("collidable", tl("fakeplayer.gui.settings.collidable")).initial(settings.collidable).build(),
            boolInput("pickupItems", tl("fakeplayer.gui.settings.pickup-items")).initial(settings.pickupItems).build(),
            boolInput("invulnerable", tl("fakeplayer.gui.settings.invulnerable")).initial(settings.invulnerable).build(),
            boolInput("autoReplenish", tl("fakeplayer.gui.settings.auto-replenish")).initial(settings.autoReplenish).build()
        )
        val onSubmitClick = DialogAction.customClick(
            { view, _ ->
                fakePlayer.settings = FakePlayerSettings(
                    view.getBoolean("collidable") ?: settings.collidable,
                    view.getBoolean("pickupItems") ?: settings.pickupItems,
                    view.getBoolean("invulnerable") ?: settings.invulnerable,
                    view.getBoolean("autoReplenish") ?: settings.autoReplenish
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

    fun actionExecuteDialog(fakePlayer: FakePlayer, onClickOnce: (() -> Unit)? = null, onClickInterval: ((Int) -> Unit)? = null, onClickContinuous: (() -> Unit)? = null, onClickStop: (() -> Unit)? = null): DialogLike {
        val actionButtons = mutableListOf<ActionButton>()
        var columns = 0
        if (onClickOnce != null) {
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.action.execute-once"), null, 100,
                DialogAction.customClick({ _, _ -> onClickOnce() }, ACTION_OPTIONS)
            ))
            columns++
        }
        if (onClickInterval != null) {
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.action.execute-interval"), null, 100,
                DialogAction.customClick({ view, _ -> onClickInterval(view.getFloat("intervalTicks")!!.toInt()) }, ACTION_OPTIONS)
            ))
            columns++
        }
        if (onClickContinuous != null) {
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.action.execute-continuous"), null, 100,
                DialogAction.customClick({ _, _ -> onClickContinuous() }, ACTION_OPTIONS)
            ))
            columns++
        }
        if (onClickStop != null) {
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.action.stop"), null, 100,
                DialogAction.customClick({ _, _ -> onClickStop() }, ACTION_OPTIONS)
            ))
        }
        val inputs = mutableListOf<DialogInput>()
        if (onClickInterval != null) {
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

    fun actionListDialog(fakePlayer: FakePlayer, vararg textAndAction: Pair<Component, () -> Unit>): DialogLike {
        val actionButtons = textAndAction.map { (text, action) ->
            ActionButton.create(text, null, 100, DialogAction.customClick({ _, _ -> action() }, ACTION_OPTIONS) )
        }
        return Dialog.create { builder ->  builder.empty()
           .base(DialogBase.builder(tl("fakeplayer.gui.action.title", fakePlayer.name)).canCloseWithEscape(true).build())
           .type(DialogType.multiAction(actionButtons).columns(1).exitAction(CANCEL_BTN).build())
        }
    }

}