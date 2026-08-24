package com.coderxi.plugin.fakeplayer.component

import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionMode
import com.coderxi.plugin.fakeplayer.api.action.ActionMode.*
import com.coderxi.plugin.fakeplayer.api.action.ActionType
import com.coderxi.plugin.fakeplayer.api.config.FakePlayerSettings
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.utils.ParamName
import com.coderxi.plugin.fakeplayer.utils.tl
import com.coderxi.plugin.fakeplayer.utils.tls
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange
import io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange
import io.papermc.paper.registry.data.dialog.input.DialogInput.bool as boolInput
import io.papermc.paper.registry.data.dialog.input.DialogInput.text as textInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.dialog.DialogLike
import net.kyori.adventure.text.Component
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ACTION
import com.coderxi.plugin.fakeplayer.command.permission.Permission.ADMIN
import com.coderxi.plugin.fakeplayer.command.permission.Permission.BASIC
import com.coderxi.plugin.fakeplayer.command.permission.Permission.SPAWN_WITH_NAME
import com.coderxi.plugin.fakeplayer.utils.hasPermission

import com.coderxi.plugin.fakeplayer.component.FakePlayerSelector.selected
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.command.CommandSender
import java.time.Duration

@Suppress("UnstableApiUsage")
object FakePlayerDialog {

    private val CANCEL_BTN by lazy { ActionButton.create(tl("fakeplayer.gui.cancel"),null, 100, null) }
    private val ACTION_OPTIONS by lazy { ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build() }
    private val EMPTY_TEXT by lazy { DialogBody.plainMessage(Component.text(" ")) }

    fun settingsDialog(viewer: CommandSender, fakePlayer: FakePlayer, onSubmit: (newName: String?) -> Unit = {}): DialogLike {
        val settings = fakePlayer.settings
        val canRename = viewer.hasPermission(SPAWN_WITH_NAME, ADMIN)
        val inputs = mutableListOf<DialogInput>()
        if (canRename) {
            inputs.add(textInput("name", tl("fakeplayer.gui.settings.name")).initial(fakePlayer.name).maxLength(16).build())
        }
        inputs.addAll(listOf(
            boolInput("collidable", tl("fakeplayer.gui.settings.collidable")).initial(settings.collidable).build(),
            boolInput("pickupItems", tl("fakeplayer.gui.settings.pickup-items")).initial(settings.pickupItems).build(),
            boolInput("invulnerable", tl("fakeplayer.gui.settings.invulnerable")).initial(settings.invulnerable).build(),
            boolInput("autoReplenish", tl("fakeplayer.gui.settings.auto-replenish")).initial(settings.autoReplenish).build(),
            boolInput("autoFish", tl("fakeplayer.gui.settings.auto-fish")).initial(settings.autoFish).build(),
        ))
        val onSubmitClick = DialogAction.customClick(
            { view, _ ->
                fakePlayer.settings = FakePlayerSettings(
                    view.getBoolean("collidable") ?: settings.collidable,
                    view.getBoolean("pickupItems") ?: settings.pickupItems,
                    view.getBoolean("invulnerable") ?: settings.invulnerable,
                    view.getBoolean("autoReplenish") ?: settings.autoReplenish,
                    view.getBoolean("autoFish") ?: settings.autoFish,
                )
                val newName = if (canRename) view.getText("name")?.trim() else null
                onSubmit.invoke(newName)
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

    fun actionExecuteDialog(viewer: CommandSender, fakePlayer: FakePlayer, actionType: ActionType): DialogLike {
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
                   if (action is com.coderxi.plugin.fakeplayer.api.action.FlattenAction && viewer is org.bukkit.entity.Player) {
                       val selection = FlattenSelectionManager.getSelection(viewer)
                       if (selection != null && selection.isComplete) {
                           action.world = selection.pos1!!.world
                           action.minX = selection.minX
                           action.maxX = selection.maxX
                           action.minY = selection.minY
                           action.maxY = selection.maxY
                           action.minZ = selection.minZ
                           action.maxZ = selection.maxZ
                           FlattenSelectionManager.stopSelectingMode(viewer)
                           viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.start", fakePlayer.name, selection.blockCount))
                       }
                   }
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

    private val autoRefreshEnabledMap = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Boolean>()
    private val autoRefreshTasks = java.util.concurrent.ConcurrentHashMap<java.util.UUID, Any>()

    fun startAutoRefresh(viewer: org.bukkit.entity.Player, fakePlayer: FakePlayer) {
        stopAutoRefresh(viewer)
        if (!autoRefreshEnabledMap.getOrDefault(viewer.uniqueId, true)) return

        if (com.coderxi.plugin.fakeplayer.utils.isFolia) {
            val task = viewer.scheduler.runAtFixedRate(com.coderxi.plugin.fakeplayer.utils.plugin, { task ->
                if (!viewer.isOnline) {
                    task.cancel()
                    autoRefreshTasks.remove(viewer.uniqueId)
                    return@runAtFixedRate
                }
                val currentAction = fakePlayer.actions.getActiveActions()[ActionType.FLATTEN.track] as? com.coderxi.plugin.fakeplayer.api.action.FlattenAction
                if (currentAction == null || !autoRefreshEnabledMap.getOrDefault(viewer.uniqueId, true)) {
                    task.cancel()
                    autoRefreshTasks.remove(viewer.uniqueId)
                    return@runAtFixedRate
                }
                viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer, isAutoLoop = true))
            }, null, 20L, 20L)
            task?.let { autoRefreshTasks[viewer.uniqueId] = it }
        } else {
            val task = com.coderxi.plugin.fakeplayer.utils.plugin.server.scheduler.runTaskTimer(com.coderxi.plugin.fakeplayer.utils.plugin, Runnable {
                if (!viewer.isOnline) {
                    stopAutoRefresh(viewer)
                    return@Runnable
                }
                val currentAction = fakePlayer.actions.getActiveActions()[ActionType.FLATTEN.track] as? com.coderxi.plugin.fakeplayer.api.action.FlattenAction
                if (currentAction == null || !autoRefreshEnabledMap.getOrDefault(viewer.uniqueId, true)) {
                    stopAutoRefresh(viewer)
                    return@Runnable
                }
                viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer, isAutoLoop = true))
            }, 20L, 20L)
            task?.let { autoRefreshTasks[viewer.uniqueId] = it }
        }
    }

    fun stopAutoRefresh(viewer: org.bukkit.entity.Player) {
        val task = autoRefreshTasks.remove(viewer.uniqueId) ?: return
        if (com.coderxi.plugin.fakeplayer.utils.isFolia) {
            (task as? io.papermc.paper.threadedregions.scheduler.ScheduledTask)?.cancel()
        } else {
            (task as? org.bukkit.scheduler.BukkitTask)?.cancel()
        }
    }

    private fun runOnPlayerThread(viewer: org.bukkit.entity.Player, runnable: Runnable) {
        if (com.coderxi.plugin.fakeplayer.utils.isFolia) {
            viewer.scheduler.run(com.coderxi.plugin.fakeplayer.utils.plugin, { _ -> runnable.run() }, null)
        } else {
            com.coderxi.plugin.fakeplayer.utils.plugin.server.scheduler.runTask(com.coderxi.plugin.fakeplayer.utils.plugin, runnable)
        }
    }

    fun flattenDialog(viewer: org.bukkit.entity.Player, fakePlayer: FakePlayer, isAutoLoop: Boolean = false): DialogLike {
        viewer.selected = fakePlayer
        val currentAction = fakePlayer.actions.getActiveActions()[ActionType.FLATTEN.track] as? com.coderxi.plugin.fakeplayer.api.action.FlattenAction
        val isRunning = currentAction != null
        val selection = FlattenSelectionManager.getSelection(viewer)
        val isComplete = selection != null && selection.isComplete
        val actionButtons = mutableListOf<ActionButton>()
        val inputs = mutableListOf<DialogInput>()

        if (isRunning) {
            if (!isAutoLoop) {
                startAutoRefresh(viewer, fakePlayer)
            }

            val total = currentAction.totalBlocks
            val cleared = currentAction.clearedBlocks
            val percent = if (total > 0) (cleared * 100 / total) else 0
            val targetName = currentAction.target?.type?.name ?: currentAction.lastTargetName

            val bodyMsg = tl("fakeplayer.gui.flatten.status.running", percent, cleared, total, targetName)

            inputs.add(boolInput("autoRefresh", tl("fakeplayer.gui.flatten.auto-refresh")).initial(autoRefreshEnabledMap.getOrDefault(viewer.uniqueId, true)).build())

            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.btn.refresh"), null, 100,
                DialogAction.customClick({ view, _ ->
                    val auto = view.getBoolean("autoRefresh") ?: true
                    autoRefreshEnabledMap[viewer.uniqueId] = auto
                    if (auto) {
                        startAutoRefresh(viewer, fakePlayer)
                    } else {
                        stopAutoRefresh(viewer)
                    }
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                    }
                }, ACTION_OPTIONS)
            ))

            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.action.stop"), null, 100,
                DialogAction.customClick({ _, _ ->
                    stopAutoRefresh(viewer)
                    fakePlayer.actions.stop(ActionType.FLATTEN.track)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                    }
                }, ACTION_OPTIONS)
            ))

            val cancelExitBtn = ActionButton.create(
                tl("fakeplayer.gui.cancel"), null, 100,
                DialogAction.customClick({ _, _ ->
                    stopAutoRefresh(viewer)
                }, ACTION_OPTIONS)
            )

            return Dialog.create { builder -> builder.empty()
                .base(DialogBase.builder(tl("fakeplayer.gui.flatten.title", fakePlayer.name))
                    .canCloseWithEscape(true)
                    .body(listOf(DialogBody.plainMessage(bodyMsg)))
                    .inputs(inputs)
                    .build())
                .type(DialogType.multiAction(actionButtons).columns(2).exitAction(cancelExitBtn).build())
            }
        }

        // 未在整地中的面板
        inputs.add(boolInput("preserveOres", tl("fakeplayer.gui.flatten.preserve-ores")).initial(false).build())
        inputs.add(boolInput("pickupItems", tl("fakeplayer.gui.flatten.pickup-items")).initial(true).build())
        val hasChests = selection != null && selection.chestBlocks.isNotEmpty()
        inputs.add(boolInput("autoDeposit", tl("fakeplayer.gui.flatten.auto-deposit")).initial(hasChests).build())

        actionButtons.add(ActionButton.create(
            tl("fakeplayer.gui.flatten.btn.select"), null, 100,
            DialogAction.customClick({ _, _ ->
                FlattenSelectionManager.startSelection(viewer)
                viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.select.mode"))
            }, ACTION_OPTIONS)
        ))

        val manageChestsLabel = if (hasChests) {
            tl("fakeplayer.gui.flatten.btn.manage-chests-count", selection.chestBlocks.size)
        } else {
            tl("fakeplayer.gui.flatten.btn.manage-chests")
        }

        actionButtons.add(ActionButton.create(
            manageChestsLabel, null, 100,
            DialogAction.customClick({ _, _ ->
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        ))

        if (isComplete && selection != null) {
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.btn.clear"), null, 100,
                DialogAction.customClick({ _, _ ->
                    FlattenSelectionManager.clearSelection(viewer)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                    }
                }, ACTION_OPTIONS)
            ))

            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.btn.start"), null, 100,
                DialogAction.customClick({ view, _ ->
                    val p1 = selection.pos1!!
                    val action = com.coderxi.plugin.fakeplayer.api.action.FlattenAction(ActionMode.Continuous).apply {
                        world = p1.world
                        minX = selection.minX
                        maxX = selection.maxX
                        minY = selection.minY
                        maxY = selection.maxY
                        minZ = selection.minZ
                        maxZ = selection.maxZ
                        preserveOres = view.getBoolean("preserveOres") ?: false
                        pickupItems = view.getBoolean("pickupItems") ?: true
                        autoDeposit = view.getBoolean("autoDeposit") ?: hasChests
                        selection.chestBlocks.forEach { cb ->
                            chestLocations.add(cb.location)
                        }
                        if (selection.chestBlocks.isNotEmpty()) {
                            val first = selection.chestBlocks.first()
                            chestWorld = first.world
                            chestX = first.x
                            chestY = first.y
                            chestZ = first.z
                        }
                    }
                    fakePlayer.actions.dispatch(action)
                    FlattenSelectionManager.stopSelectingMode(viewer)
                    viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.start", fakePlayer.name, selection.blockCount))
                }, ACTION_OPTIONS)
            ))
        }

        val isHighlighting = FlattenSelectionManager.isHighlightingChests(viewer)
        val particleInfo = if (isHighlighting) tls("fakeplayer.gui.flatten.particle.on") else tls("fakeplayer.gui.flatten.particle.off")

        val chestInfo = if (selection != null && selection.chestBlocks.isNotEmpty()) {
            if (selection.chestBlocks.size == 1) {
                val cb = selection.chestBlocks.first()
                "<aqua>${cb.x}, ${cb.y}, ${cb.z}</aqua> <dark_gray>(${cb.type.name})</dark_gray>"
            } else {
                tls("fakeplayer.gui.flatten.chest.multi", selection.chestBlocks.size)
            }
        } else {
            tls("fakeplayer.gui.flatten.chest.none")
        }

        val bodyMsg = if (isComplete && selection != null) {
            tl("fakeplayer.gui.flatten.status.ready",
                "${selection.pos1!!.x}, ${selection.pos1!!.y}, ${selection.pos1!!.z}",
                "${selection.pos2!!.x}, ${selection.pos2!!.y}, ${selection.pos2!!.z}",
                "${selection.sizeX} × ${selection.sizeY} × ${selection.sizeZ}",
                selection.blockCount,
                chestInfo,
                particleInfo
            )
        } else {
            tl("fakeplayer.gui.flatten.status.not-selected", chestInfo, particleInfo)
        }

        val cols = if (actionButtons.size >= 2) 2 else 1

        return Dialog.create { builder -> builder.empty()
            .base(DialogBase.builder(tl("fakeplayer.gui.flatten.title", fakePlayer.name))
                .canCloseWithEscape(true)
                .body(listOf(DialogBody.plainMessage(bodyMsg)))
                .inputs(inputs)
                .build())
            .type(DialogType.multiAction(actionButtons).columns(cols).exitAction(CANCEL_BTN).build())
        }
    }

    fun chestListDialog(viewer: org.bukkit.entity.Player, fakePlayer: FakePlayer): DialogLike {
        viewer.selected = fakePlayer
        val selection = FlattenSelectionManager.getSelection(viewer)
        val chestBlocks = selection?.chestBlocks ?: emptyList()
        val actionButtons = mutableListOf<ActionButton>()

        actionButtons.add(ActionButton.create(
            tl("fakeplayer.gui.flatten.chest.btn.add"), null, 100,
            DialogAction.customClick({ _, _ ->
                FlattenSelectionManager.startChestSelection(viewer)
                viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.chest.mode"))
            }, ACTION_OPTIONS)
        ))

        val isHighlighting = FlattenSelectionManager.isHighlightingChests(viewer)
        val highlightLabel = if (isHighlighting) {
            tl("fakeplayer.gui.flatten.chest.btn.highlight-on")
        } else {
            tl("fakeplayer.gui.flatten.chest.btn.highlight-off")
        }
        actionButtons.add(ActionButton.create(
            highlightLabel, null, 100,
            DialogAction.customClick({ _, _ ->
                FlattenSelectionManager.toggleHighlightingChests(viewer)
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        ))

        chestBlocks.forEachIndexed { index, cb ->
            val state = cb.state
            val isDouble = state is org.bukkit.block.Chest && state.inventory.holder is org.bukkit.block.DoubleChest
            val typeName = if (isDouble) "DOUBLE_CHEST" else cb.type.name
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.chest.btn.remove", index + 1, typeName, "${cb.x}, ${cb.y}, ${cb.z}"), null, 100,
                DialogAction.customClick({ _, _ ->
                    FlattenSelectionManager.removeChestBlock(viewer, index)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer))
                    }
                }, ACTION_OPTIONS)
            ))
        }

        if (chestBlocks.isNotEmpty()) {
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.chest.btn.clear-all"), null, 100,
                DialogAction.customClick({ _, _ ->
                    FlattenSelectionManager.clearChestBlocks(viewer)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer))
                    }
                }, ACTION_OPTIONS)
            ))
        }

        val backBtn = ActionButton.create(
            tl("fakeplayer.gui.back-to-flatten"), null, 100,
            DialogAction.customClick({ _, _ ->
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        )

        val bodyMsg = if (chestBlocks.isEmpty()) {
            tl("fakeplayer.gui.flatten.chest.list.empty")
        } else {
            val listStr = chestBlocks.mapIndexed { i, cb ->
                val state = cb.state
                val isDouble = state is org.bukkit.block.Chest && state.inventory.holder is org.bukkit.block.DoubleChest
                val typeName = if (isDouble) "DOUBLE_CHEST" else cb.type.name
                "<gray> • <gold>#${i + 1}</gold> <white>$typeName</white> <aqua>(${cb.x}, ${cb.y}, ${cb.z})</aqua></gray>"
            }.joinToString("\n")
            val effectStatus = if (isHighlighting) tls("fakeplayer.gui.flatten.particle.on") else tls("fakeplayer.gui.flatten.particle.off")
            tl("fakeplayer.gui.flatten.chest.list.header", chestBlocks.size, listStr, effectStatus)
        }

        return Dialog.create { builder -> builder.empty()
            .base(DialogBase.builder(tl("fakeplayer.gui.flatten.chest.title", fakePlayer.name))
                .canCloseWithEscape(true)
                .body(listOf(DialogBody.plainMessage(bodyMsg)))
                .build())
            .type(DialogType.multiAction(actionButtons).columns(1).exitAction(backBtn).build())
        }
    }

    fun actionListDialog(viewer: CommandSender, fakePlayer: FakePlayer): DialogLike {
        val actionTypes = if (viewer.hasPermission(BASIC)) ActionType.entries else ActionType.entries.filter { viewer.hasPermission("${ACTION.value}.${it.name.lowercase()}") }
        val textAndAction = actionTypes
            .associateTo((mutableMapOf())) { type ->
                tl("fakeplayer.action.${type.name.lowercase().replace("_","-")}") to {
                    if (type == ActionType.FLATTEN && viewer is org.bukkit.entity.Player) {
                        viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                    } else {
                        viewer.showDialog(FakePlayerDialog.actionExecuteDialog(viewer, fakePlayer, type))
                    }
                }
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