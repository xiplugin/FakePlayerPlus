package com.coderxi.plugin.fakeplayer.utils

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.dialog.DialogResponseView
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration
import kotlin.reflect.KMutableProperty0

@Suppress("UnstableApiUsage")
open class FormDialog(val title: Component) {

    private val defaultButtonWidth = 120

    private class FormEntry(
        val permissions: Collection<String>?,
        val build: () -> DialogInput,
        val onSubmit: (DialogResponseView) -> Unit
    )

    private val entries = mutableListOf<FormEntry>()

    fun bool(
        property: KMutableProperty0<Boolean>,
        label: Component = Component.text(property.name),
        permissions: Collection<String>? = null,
        onChange: ((newValue: Boolean) -> Unit)? = null
    ): FormDialog = apply {
        entries.add(FormEntry(
            permissions = permissions,
            build = { DialogInput.bool(property.name, label).initial(property.get()).build() },
            onSubmit = { view ->
                val newValue = view.getBoolean(property.name)
                if (newValue != null && newValue != property.get()) {
                    property.set(newValue)
                    onChange?.invoke(newValue)
                }
            }
        ))
    }

    fun <T> singleOption(
        property: KMutableProperty0<T>,
        label: Component = Component.text(property.name),
        options: List<Pair<T, Component>>,
        width: Int = defaultButtonWidth,
        permissions: Collection<String>? = null,
        onChange: ((newValue: T) -> Unit)? = null
    ): FormDialog = apply {
        entries.add(FormEntry(
            permissions = permissions,
            build = {
                val currentValue = property.get()
                val optionEntries = options.mapIndexed { index, (value, optionLabel) ->
                    SingleOptionDialogInput.OptionEntry.create(
                        index.toString(),
                        optionLabel,
                        value == currentValue
                    )
                }
                DialogInput.singleOption(property.name, label, optionEntries).width(width).build()
            },
            onSubmit = { view ->
                val selectedId = view.getText(property.name) ?: return@FormEntry
                val selectedValue = options.getOrNull(selectedId.toInt())?.first
                if (selectedValue != null && selectedValue != property.get()) {
                    property.set(selectedValue)
                    onChange?.invoke(selectedValue)
                }
            }
        ))
    }

    fun boolSingleOption(
        property: KMutableProperty0<Boolean>,
        label: Component = Component.text(property.name),
        trueLabel: Component = tl("fakeplayer.gui.var.true"),
        falseLabel: Component = tl("fakeplayer.gui.var.false"),
        width: Int = defaultButtonWidth,
        permissions: Collection<String>? = null,
        onChange: ((newValue: Boolean) -> Unit)? = null
    ): FormDialog {
        val options = listOf(true to trueLabel, false to falseLabel)
        return singleOption(property, label, options, width, permissions, onChange)
    }

    fun <E : Enum<E>> enumSingleOption(
        enumClass: Class<E>,
        property: KMutableProperty0<E>,
        label: Component = Component.text(property.name),
        width: Int = defaultButtonWidth,
        permissions: Collection<String>? = null,
        optionLabelProvider: (E) -> Component = { Component.text(it.name) },
        onChange: ((newValue: E) -> Unit)? = null
    ): FormDialog {
        val options = enumClass.enumConstants.map { it to optionLabelProvider(it) }
        return singleOption(property, label, options, width, permissions, onChange)
    }

    fun numberRange(
        property: KMutableProperty0<Float>,
        label: Component = Component.text(property.name),
        start: Float,
        end: Float,
        step: Float = 0.5f,
        width: Int = defaultButtonWidth,
        permissions: Collection<String>? = null,
        onChange: ((newValue: Float) -> Unit)? = null
    ): FormDialog = apply {
        entries.add(FormEntry(
            permissions = permissions,
            build = {
                DialogInput.numberRange(property.name, label, start, end)
                    .step(step)
                    .initial(property.get().coerceAtMost(end))
                    .width(width)
                    .build()
            },
            onSubmit = { view ->
                val newValue = view.getFloat(property.name)
                if (newValue != null && newValue != property.get()) {
                    property.set(newValue)
                    onChange?.invoke(newValue)
                }
            }
        ))
    }

    fun numberRange(
        property: KMutableProperty0<Int>,
        label: Component = Component.text(property.name),
        labelFormat: String? = null,
        start: Int,
        end: Int,
        step: Int = 1,
        width: Int = defaultButtonWidth,
        permissions: Collection<String>? = null,
        onChange: ((newValue: Int) -> Unit)? = null
    ): FormDialog = apply {
        entries.add(FormEntry(
            permissions = permissions,
            build = {
                DialogInput.numberRange(property.name, label, start.toFloat(), end.toFloat())
                    .step(step.toFloat())
                    .initial(property.get().toFloat().coerceAtMost(end.toFloat()))
                    .width(width)
                    .apply { if(labelFormat != null) labelFormat(labelFormat) }
                    .build()
            },
            onSubmit = { view ->
                val newValue = view.getFloat(property.name)?.toInt()
                if (newValue != null && newValue != property.get()) {
                    property.set(newValue)
                    onChange?.invoke(newValue)
                }
            }
        ))
    }

    fun numberRange(
        key: String,
        initial: Int,
        label: Component = Component.text(key),
        labelFormat: String? = null,
        start: Int,
        end: Int,
        step: Int = 1,
        width: Int = defaultButtonWidth,
        permissions: Collection<String>? = null,
        onChange: ((newValue: Int) -> Unit)? = null
    ): FormDialog = apply {
        entries.add(FormEntry(
            permissions = permissions,
            build = {
                DialogInput.numberRange(key, label, start.toFloat(), end.toFloat())
                    .step(step.toFloat())
                    .initial(initial.toFloat())
                    .width(width)
                    .apply { if(labelFormat != null) labelFormat(labelFormat) }
                    .build()
            },
            onSubmit = { view ->
                val newValue = view.getFloat(key)?.toInt()
                if (newValue != null && newValue != initial) {
                    onChange?.invoke(newValue)
                }
            }
        ))
    }

    fun text(
        property: KMutableProperty0<String>,
        label: Component = Component.text(property.name),
        width: Int = defaultButtonWidth,
        maxLength: Int = 16,
        permissions: Collection<String>? = null,
        onChange: ((newValue: String) -> Unit)? = null
    ): FormDialog = apply {
        entries.add(FormEntry(
            permissions = permissions,
            build = {
                DialogInput.text(property.name, label)
                    .initial(property.get())
                    .width(width)
                    .maxLength(maxLength)
                    .build()
            },
            onSubmit = { view ->
                val newValue = view.getText(property.name)
                if (newValue != null && newValue != property.get()) {
                    property.set(newValue)
                    onChange?.invoke(newValue)
                }
            }
        ))
    }

    fun text(
        key: String,
        initial: String,
        label: Component = Component.text(key),
        width: Int = defaultButtonWidth,
        maxLength: Int = 16,
        permissions: Collection<String>? = null,
        onChange: ((newValue: String) -> Unit)? = null
    ): FormDialog = apply {
        entries.add(FormEntry(
            permissions = permissions,
            build = {
                DialogInput.text(key, label)
                    .initial(initial)
                    .width(width)
                    .maxLength(maxLength)
                    .build()
            },
            onSubmit = { view ->
                val newValue = view.getText(key)
                if (newValue != null && newValue != initial) {
                    onChange?.invoke(newValue)
                }
            }
        ))
    }

    private val actionButtons = mutableListOf<ActionButton>()
    private var actionButtonColumns: Int? = null

    fun actionButton(button: ActionButton): FormDialog {
        actionButtons.add(button)
        return this
    }

    fun actionButtonColumns(columns: Int): FormDialog {
        actionButtonColumns = columns
        return this
    }

    private var submitButton : ActionButton? = null
    fun submitButton(
        label: Component = tl("fakeplayer.gui.submit"),
        width: Int = defaultButtonWidth,
        options: ClickCallback.Options = defaultActionOptions,
        onClick: (() -> Unit)? = null
    ): FormDialog {
        submitButton = ActionButton.create(label, null, width,
            DialogAction.customClick({ view, _ -> entries.forEach { it.onSubmit(view) }; onClick?.invoke() }, options)
        )
        return this
    }

    private var cancelButton : ActionButton? = null
    fun cancelButton(
        label: Component = tl("fakeplayer.gui.cancel"),
        width: Int = defaultButtonWidth,
        options: ClickCallback.Options = defaultActionOptions,
        onClick: (() -> Unit)? = null
    ): FormDialog {
        cancelButton = ActionButton.create(label, null, width,
            DialogAction.customClick({ _, _ ->  onClick?.invoke() }, options)
        )
        return this
    }

    fun show(player: Player, playSound: Boolean = true) {
        val formButtons = listOfNotNull(submitButton, cancelButton)
        val allButtons = actionButtons + formButtons
        val dialog = Dialog.create { builder -> builder.empty()
                .base(
                    DialogBase.builder(title)
                        .canCloseWithEscape(true)
                        .inputs(entries.filter { it.permissions == null || it.permissions.any(player::hasPermission)}.map { it.build() })
                        .build()
                )
                .type(
                    if (actionButtons.isEmpty() && submitButton != null) {
                        if (cancelButton == null) cancelButton()
                        DialogType.confirmation(submitButton!!, cancelButton!!)
                    } else {
                        val columns = actionButtonColumns ?: (allButtons.size - formButtons.size).coerceAtLeast(formButtons.size)
                        DialogType.multiAction(allButtons).columns(columns).build()
                    }
                )
        }
        player.showDialog(dialog)
        if (playSound) player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.3f, 1f)
    }

    protected val defaultActionOptions: ClickCallback.Options by lazy {
        ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build()
    }

}