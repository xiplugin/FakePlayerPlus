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
        val isOpLevel4 = viewer is org.bukkit.command.ConsoleCommandSender || (viewer is org.bukkit.entity.Player && (viewer.isOp || viewer.hasPermission(ADMIN)))
        if (isOpLevel4) {
            inputs.add(boolInput("autoRejoin", tl("fakeplayer.gui.settings.auto-rejoin")).initial(settings.autoRejoin).build())
        }
        val onSubmitClick = DialogAction.customClick(
            { view, _ ->
                val autoRejoin = if (isOpLevel4) (view.getBoolean("autoRejoin") ?: settings.autoRejoin) else settings.autoRejoin
                fakePlayer.settings = FakePlayerSettings(
                    view.getBoolean("collidable") ?: settings.collidable,
                    view.getBoolean("pickupItems") ?: settings.pickupItems,
                    view.getBoolean("invulnerable") ?: settings.invulnerable,
                    view.getBoolean("autoReplenish") ?: settings.autoReplenish,
                    view.getBoolean("autoFish") ?: settings.autoFish,
                    autoRejoin
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
        val selection = FlattenSelectionManager.getOrCreateSelection(viewer)
        val isComplete = selection.isComplete
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

            val fpm = com.coderxi.plugin.fakeplayer.utils.plugin.fakePlayerManager
            val allActiveWorkers = fpm.fakeplayers().filter { fp ->
                fp.actions.getActiveActions()[ActionType.FLATTEN.track] is com.coderxi.plugin.fakeplayer.api.action.FlattenAction
            }

            val stopLabel = if (allActiveWorkers.size > 1) tl("fakeplayer.gui.flatten.btn.stop-this") else tl("fakeplayer.gui.action.stop")
            actionButtons.add(ActionButton.create(
                stopLabel, null, 100,
                DialogAction.customClick({ _, _ ->
                    stopAutoRefresh(viewer)
                    fakePlayer.actions.stop(ActionType.FLATTEN.track)
                    fakePlayer.actions.stop(com.coderxi.plugin.fakeplayer.api.action.ActionTrack.INTERACTION)
                    com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.release(fakePlayer.uuid)
                    com.coderxi.plugin.fakeplayer.repository.FlattenRepository().deleteTask(fakePlayer.uuid)
                    viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.stopped", fakePlayer.name))
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                    }
                }, ACTION_OPTIONS)
            ))

            if (allActiveWorkers.size > 1) {
                actionButtons.add(ActionButton.create(
                    tl("fakeplayer.gui.flatten.btn.stop-all-workers", allActiveWorkers.size), null, 100,
                    DialogAction.customClick({ _, _ ->
                        stopAutoRefresh(viewer)
                        for (w in allActiveWorkers) {
                            w.actions.stop(ActionType.FLATTEN.track)
                            w.actions.stop(com.coderxi.plugin.fakeplayer.api.action.ActionTrack.INTERACTION)
                            com.coderxi.plugin.fakeplayer.component.FlattenTargetRegistry.release(w.uuid)
                            com.coderxi.plugin.fakeplayer.repository.FlattenRepository().deleteTask(w.uuid)
                        }
                        viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.stopped.all", allActiveWorkers.size))
                        runOnPlayerThread(viewer) {
                            viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                        }
                    }, ACTION_OPTIONS)
                ))
            }

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
        val hasChests = selection.chestBlocks.isNotEmpty()
        inputs.add(boolInput("preserveOres", tl("fakeplayer.gui.flatten.preserve-ores")).initial(selection.preserveOres).build())
        inputs.add(boolInput("pickupItems", tl("fakeplayer.gui.flatten.pickup-items")).initial(selection.pickupItems).build())
        inputs.add(boolInput("autoDeposit", tl("fakeplayer.gui.flatten.auto-deposit")).initial(selection.autoDeposit).build())

        val activeWorkers = selectedWorkersMap.computeIfAbsent(viewer.uniqueId) { mutableSetOf(fakePlayer.uuid) }
        if (!activeWorkers.contains(fakePlayer.uuid)) {
            activeWorkers.add(fakePlayer.uuid)
        }
        val availableWorkers = getAvailableWorkers(viewer)
        val validWorkerCount = activeWorkers.count { wUuid -> availableWorkers.any { it.uuid == wUuid } }

        actionButtons.add(ActionButton.create(
            tl("fakeplayer.gui.flatten.btn.select"), null, 100,
            DialogAction.customClick({ view, _ ->
                view.getBoolean("preserveOres")?.let { selection.preserveOres = it }
                view.getBoolean("pickupItems")?.let { selection.pickupItems = it }
                view.getBoolean("autoDeposit")?.let { selection.autoDeposit = it }
                FlattenSelectionManager.saveSelection(viewer)
                FlattenSelectionManager.startSelection(viewer)
                viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.select.mode"))
            }, ACTION_OPTIONS)
        ))

        val outputChestsLabel = tl("fakeplayer.gui.flatten.btn.output-chests", selection.outputChests.size)
        actionButtons.add(ActionButton.create(
            outputChestsLabel, null, 100,
            DialogAction.customClick({ view, _ ->
                view.getBoolean("preserveOres")?.let { selection.preserveOres = it }
                view.getBoolean("pickupItems")?.let { selection.pickupItems = it }
                view.getBoolean("autoDeposit")?.let { selection.autoDeposit = it }
                FlattenSelectionManager.saveSelection(viewer)
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer, ChestRole.OUTPUT))
                }
            }, ACTION_OPTIONS)
        ))

        val toolChestsLabel = tl("fakeplayer.gui.flatten.btn.tool-chests", selection.toolChests.size)
        actionButtons.add(ActionButton.create(
            toolChestsLabel, null, 100,
            DialogAction.customClick({ view, _ ->
                view.getBoolean("preserveOres")?.let { selection.preserveOres = it }
                view.getBoolean("pickupItems")?.let { selection.pickupItems = it }
                view.getBoolean("autoDeposit")?.let { selection.autoDeposit = it }
                FlattenSelectionManager.saveSelection(viewer)
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer, ChestRole.TOOL))
                }
            }, ACTION_OPTIONS)
        ))

        val workersLabel = tl("fakeplayer.gui.flatten.btn.workers", validWorkerCount)
        actionButtons.add(ActionButton.create(
            workersLabel, null, 100,
            DialogAction.customClick({ view, _ ->
                view.getBoolean("preserveOres")?.let { selection.preserveOres = it }
                view.getBoolean("pickupItems")?.let { selection.pickupItems = it }
                view.getBoolean("autoDeposit")?.let { selection.autoDeposit = it }
                FlattenSelectionManager.saveSelection(viewer)
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.collaborativeWorkersDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        ))

        if (isComplete) {
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.btn.clear"), null, 100,
                DialogAction.customClick({ view, _ ->
                    view.getBoolean("preserveOres")?.let { selection.preserveOres = it }
                    view.getBoolean("pickupItems")?.let { selection.pickupItems = it }
                    view.getBoolean("autoDeposit")?.let { selection.autoDeposit = it }
                    FlattenSelectionManager.clearSelection(viewer)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                    }
                }, ACTION_OPTIONS)
            ))

            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.btn.start"), null, 100,
                DialogAction.customClick({ view, _ ->
                    view.getBoolean("preserveOres")?.let { selection.preserveOres = it }
                    view.getBoolean("pickupItems")?.let { selection.pickupItems = it }
                    view.getBoolean("autoDeposit")?.let { selection.autoDeposit = it }
                    FlattenSelectionManager.saveSelection(viewer)
                    val p1 = selection.pos1!!

                    val fpm = com.coderxi.plugin.fakeplayer.utils.plugin.fakePlayerManager
                    val targetWorkers = activeWorkers.mapNotNull { fpm.get(it) }.filter { it.player.isOnline }
                    val workersToDispatch = if (targetWorkers.isNotEmpty()) targetWorkers else listOf(fakePlayer)

                    for (worker in workersToDispatch) {
                        val action = com.coderxi.plugin.fakeplayer.api.action.FlattenAction(ActionMode.Continuous).apply {
                            world = p1.world
                            minX = selection.minX
                            maxX = selection.maxX
                            minY = selection.minY
                            maxY = selection.maxY
                            minZ = selection.minZ
                            maxZ = selection.maxZ
                            this.preserveOres = selection.preserveOres
                            this.pickupItems = selection.pickupItems
                            this.autoDeposit = selection.autoDeposit
                            selection.outputChests.forEach { cb ->
                                outputChestLocations.add(cb.location)
                                chestLocations.add(cb.location)
                            }
                            selection.toolChests.forEach { cb ->
                                toolChestLocations.add(cb.location)
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
                        worker.actions.dispatch(action)
                        com.coderxi.plugin.fakeplayer.repository.FlattenRepository().saveTask(worker.uuid, action)
                    }

                    FlattenSelectionManager.stopSelectingMode(viewer)
                    if (workersToDispatch.size > 1) {
                        viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.start.multi", workersToDispatch.size, selection.blockCount))
                    } else {
                        viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp("fakeplayer.flatten.start", fakePlayer.name, selection.blockCount))
                    }
                }, ACTION_OPTIONS)
            ))
        }

        val isHighlighting = FlattenSelectionManager.isHighlightingChests(viewer)
        val particleInfo = if (isHighlighting) tls("fakeplayer.gui.flatten.particle.on") else tls("fakeplayer.gui.flatten.particle.off")

        val chestInfo = "<gold>${selection.outputChests.size}</gold> 產物箱 / <green>${selection.toolChests.size}</green> 工具箱"

        val bodyMsg = if (isComplete) {
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

        val exitBtn = ActionButton.create(
            tl("fakeplayer.gui.cancel"), null, 100,
            DialogAction.customClick({ view, _ ->
                view.getBoolean("preserveOres")?.let { selection.preserveOres = it }
                view.getBoolean("pickupItems")?.let { selection.pickupItems = it }
                view.getBoolean("autoDeposit")?.let { selection.autoDeposit = it }
            }, ACTION_OPTIONS)
        )

        return Dialog.create { builder -> builder.empty()
            .base(DialogBase.builder(tl("fakeplayer.gui.flatten.title", fakePlayer.name))
                .canCloseWithEscape(true)
                .body(listOf(DialogBody.plainMessage(bodyMsg)))
                .inputs(inputs)
                .build())
            .type(DialogType.multiAction(actionButtons).columns(cols).exitAction(exitBtn).build())
        }
    }

    fun chestListDialog(viewer: org.bukkit.entity.Player, fakePlayer: FakePlayer, role: ChestRole = ChestRole.OUTPUT): DialogLike {
        viewer.selected = fakePlayer
        val selection = FlattenSelectionManager.getSelection(viewer)
        val targetChests = if (role == ChestRole.TOOL) selection?.toolChests ?: emptyList() else selection?.outputChests ?: emptyList()
        val actionButtons = mutableListOf<ActionButton>()

        val addBtnLabel = if (role == ChestRole.TOOL) tl("fakeplayer.gui.flatten.chest.tool.btn.add") else tl("fakeplayer.gui.flatten.chest.output.btn.add")
        actionButtons.add(ActionButton.create(
            addBtnLabel, null, 100,
            DialogAction.customClick({ _, _ ->
                FlattenSelectionManager.startChestSelection(viewer, role)
                val modeMsg = if (role == ChestRole.TOOL) "fakeplayer.flatten.chest.tool.mode" else "fakeplayer.flatten.chest.output.mode"
                viewer.sendMessage(com.coderxi.plugin.fakeplayer.utils.tlp(modeMsg))
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
                    viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer, role))
                }
            }, ACTION_OPTIONS)
        ))

        targetChests.forEachIndexed { index, cb ->
            actionButtons.add(ActionButton.create(
                tl("fakeplayer.gui.flatten.btn.remove", index + 1), null, 100,
                DialogAction.customClick({ _, _ ->
                    FlattenSelectionManager.removeChestBlock(viewer, index, role)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer, role))
                    }
                }, ACTION_OPTIONS)
            ))

            val switchLabel = if (role == ChestRole.TOOL) {
                tl("fakeplayer.gui.flatten.chest.btn.switch-to-output", index + 1)
            } else {
                tl("fakeplayer.gui.flatten.chest.btn.switch-to-tool", index + 1)
            }
            actionButtons.add(ActionButton.create(
                switchLabel, null, 100,
                DialogAction.customClick({ _, _ ->
                    FlattenSelectionManager.switchChestRole(viewer, index, role)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer, role))
                    }
                }, ACTION_OPTIONS)
            ))
        }

        if (targetChests.isNotEmpty()) {
            val clearLabel = if (role == ChestRole.TOOL) tl("fakeplayer.gui.flatten.chest.tool.btn.clear-all") else tl("fakeplayer.gui.flatten.chest.output.btn.clear-all")
            actionButtons.add(ActionButton.create(
                clearLabel, null, 100,
                DialogAction.customClick({ _, _ ->
                    FlattenSelectionManager.clearChestBlocks(viewer, role)
                    runOnPlayerThread(viewer) {
                        viewer.showDialog(FakePlayerDialog.chestListDialog(viewer, fakePlayer, role))
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

        val dialogTitle = if (role == ChestRole.TOOL) {
            tl("fakeplayer.gui.flatten.chest.tool.title", fakePlayer.name)
        } else {
            tl("fakeplayer.gui.flatten.chest.output.title", fakePlayer.name)
        }

        val bodyMsg = if (targetChests.isEmpty()) {
            if (role == ChestRole.TOOL) tl("fakeplayer.gui.flatten.chest.tool.empty") else tl("fakeplayer.gui.flatten.chest.output.empty")
        } else {
            val listStr = targetChests.mapIndexed { i, cb ->
                val state = cb.state
                val isDouble = state is org.bukkit.block.Chest && state.inventory.holder is org.bukkit.block.DoubleChest
                val typeName = if (isDouble) "DOUBLE_CHEST" else cb.type.name
                "<gray> • <gold>#${i + 1}</gold> <white>$typeName</white> <aqua>(${cb.x}, ${cb.y}, ${cb.z})</aqua></gray>"
            }.joinToString("\n")
            val effectStatus = if (isHighlighting) tls("fakeplayer.gui.flatten.particle.on") else tls("fakeplayer.gui.flatten.particle.off")
            if (role == ChestRole.TOOL) {
                tl("fakeplayer.gui.flatten.chest.tool.header", targetChests.size, listStr, effectStatus)
            } else {
                tl("fakeplayer.gui.flatten.chest.output.header", targetChests.size, listStr, effectStatus)
            }
        }

        val cols = if (actionButtons.size >= 4) 2 else 1

        return Dialog.create { builder -> builder.empty()
            .base(DialogBase.builder(dialogTitle)
                .canCloseWithEscape(true)
                .body(listOf(DialogBody.plainMessage(bodyMsg)))
                .build())
            .type(DialogType.multiAction(actionButtons).columns(cols).exitAction(backBtn).build())
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

    private val selectedWorkersMap = java.util.concurrent.ConcurrentHashMap<java.util.UUID, MutableSet<java.util.UUID>>()

    private fun getAvailableWorkers(viewer: org.bukkit.entity.Player): List<FakePlayer> {
        val fpm = com.coderxi.plugin.fakeplayer.utils.plugin.fakePlayerManager
        val all = fpm.fakeplayers()
        return if (viewer.isOp) {
            all
        } else {
            all.filter { it.ownerUuids.contains(viewer.uniqueId) || it.creatorUuid == viewer.uniqueId }
        }
    }

    fun collaborativeWorkersDialog(viewer: org.bukkit.entity.Player, fakePlayer: FakePlayer): DialogLike {
        val availableWorkers = getAvailableWorkers(viewer)
        val activeWorkers = selectedWorkersMap.computeIfAbsent(viewer.uniqueId) { mutableSetOf(fakePlayer.uuid) }
        val inputs = mutableListOf<DialogInput>()

        for ((index, worker) in availableWorkers.withIndex()) {
            val isChecked = activeWorkers.contains(worker.uuid)
            inputs.add(boolInput("worker_$index", Component.text(worker.name)).initial(isChecked).build())
        }

        val actionButtons = mutableListOf<ActionButton>()

        // 全選按鈕
        actionButtons.add(ActionButton.create(
            tl("fakeplayer.gui.flatten.workers.select-all"), null, 100,
            DialogAction.customClick({ _, _ ->
                activeWorkers.clear()
                activeWorkers.addAll(availableWorkers.map { it.uuid })
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.collaborativeWorkersDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        ))

        // 僅當前假人按鈕
        actionButtons.add(ActionButton.create(
            tl("fakeplayer.gui.flatten.workers.select-current-only"), null, 100,
            DialogAction.customClick({ _, _ ->
                activeWorkers.clear()
                activeWorkers.add(fakePlayer.uuid)
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.collaborativeWorkersDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        ))

        // 儲存並返回按鈕
        actionButtons.add(ActionButton.create(
            tl("fakeplayer.gui.submit"), null, 100,
            DialogAction.customClick({ view, _ ->
                activeWorkers.clear()
                for ((index, worker) in availableWorkers.withIndex()) {
                    if (view.getBoolean("worker_$index") == true) {
                        activeWorkers.add(worker.uuid)
                    }
                }
                if (activeWorkers.isEmpty()) {
                    activeWorkers.add(fakePlayer.uuid)
                }
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        ))

        val cancelBtn = ActionButton.create(
            tl("fakeplayer.gui.back-to-flatten"), null, 100,
            DialogAction.customClick({ view, _ ->
                activeWorkers.clear()
                for ((index, worker) in availableWorkers.withIndex()) {
                    if (view.getBoolean("worker_$index") == true) {
                        activeWorkers.add(worker.uuid)
                    }
                }
                if (activeWorkers.isEmpty()) {
                    activeWorkers.add(fakePlayer.uuid)
                }
                runOnPlayerThread(viewer) {
                    viewer.showDialog(FakePlayerDialog.flattenDialog(viewer, fakePlayer))
                }
            }, ACTION_OPTIONS)
        )

        val headerMsg = tl("fakeplayer.gui.flatten.workers.header", availableWorkers.size, activeWorkers.size)

        return Dialog.create { builder -> builder.empty()
            .base(DialogBase.builder(tl("fakeplayer.gui.flatten.workers.title"))
                .canCloseWithEscape(true)
                .body(listOf(DialogBody.plainMessage(headerMsg)))
                .inputs(inputs)
                .build())
            .type(DialogType.multiAction(actionButtons).columns(3).exitAction(cancelBtn).build())
        }
    }

}