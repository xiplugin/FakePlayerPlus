package com.coderxi.plugin.fakeplayer.nms.v1_21_11

import com.coderxi.plugin.fakeplayer.api.nms.NMSInventoryView
import io.papermc.paper.adventure.PaperAdventure
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets
import net.kyori.adventure.text.Component
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Player as NMSPlayer0
import net.minecraft.world.entity.player.Inventory as NMSInventory0
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.TooltipDisplay
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftInventoryView
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack as BukkitItemStack
import java.util.Optional

open class NMSInventoryViewImpl(viewer: Player, whom: Player, readOnly: Boolean, title: Component) : NMSInventoryView {

    companion object {
        const val TOP_SIZE = 54
        const val SELECTOR_START = 36
        const val SELECTOR_END = 44
        const val FILLER_START = 50
        const val SLOT_HELMET = 39
        const val SLOT_CHESTPLATE = 38
        const val SLOT_LEGGINGS = 37
        const val SLOT_BOOTS = 36
        const val SLOT_OFFHAND = 40
        const val CUSTOM_DATA_PANE = "inventory_pane"
        const val CUSTOM_DATA_PANE_ACTIVE = "inventory_pane_active"
        val PANE: ItemStack = CraftItemStack.asNMSCopy(BukkitItemStack(Material.WHITE_STAINED_GLASS_PANE)).apply {
            set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(true, ReferenceSortedSets.emptySet()))
            set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putBoolean(CUSTOM_DATA_PANE, true) } ))
        }
    }

    override lateinit var view: InventoryView

    init {
        val viewerHandle = (viewer as CraftPlayer).handle
        val whomHandle = (whom as CraftPlayer).handle
        var containerMenu: AbstractContainerMenu? = null
        val provider = object : MenuProvider {
            override fun getDisplayName() = PaperAdventure.asVanilla(title)
            override fun createMenu(containerId: Int, inv: NMSInventory0, player: NMSPlayer0): AbstractContainerMenu? {
                if (player !is ServerPlayer) return null
                return containerMenu(containerId, viewerHandle, whomHandle, readOnly, title).also { containerMenu = it }
            }
        }
        val result = viewerHandle.openMenu(provider)
        view = (if (result.isEmpty) null else containerMenu?.bukkitView ?: viewerHandle.containerMenu.bukkitView)!!
    }

    open fun containerMenu(containerId: Int, viewer: ServerPlayer, whom: ServerPlayer, readOnly: Boolean, title: Component) : AbstractContainerMenu {
        return object : BaseInventoryMenu(containerId,viewer, whom, readOnly, title) {
            override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: NMSPlayer0) {
                if (intercept(slotId, clickType == ClickType.QUICK_CRAFT)) return
                super.clicked(slotId, button, clickType, player)
            }
        }
    }

    abstract class BaseInventoryMenu(
        containerId: Int,
        val viewer: ServerPlayer,
        val whom: ServerPlayer,
        val readOnly: Boolean,
        private val title: Component
    ) : AbstractContainerMenu(MenuType.GENERIC_9x6, containerId) {

        private var bukkitView: CraftInventoryView<*, *>? = null

        protected fun x(col: Int) = 8 + col * 18
        protected fun y(row: Int) = 18 + row * 18

        init {
            val inventory = whom.inventory
            // 0-26: 大背包 (目标容器索引 9-35)
            for (menuSlot in 0 until 27) {
                val row = menuSlot / 9
                val col = menuSlot % 9
                addSlot(createBackedSlot(inventory, menuSlot + 9, x(col), y(row)))
            }
            // 27-35: 快捷栏 (目标容器索引 0-8)
            for (i in 0 until 9) {
                addSlot(createBackedSlot(inventory, i, x(i), y(3)))
            }
            // 36-44: 快捷栏选择器 (伪槽位)
            for (i in 0 until 9) {
                addSlot(SelectorPaneSlot(x(i), y(4), i))
            }
            // 45-49: 装备栏
            addSlot(createBackedSlot(inventory, SLOT_HELMET, x(0), y(5)))
            addSlot(createBackedSlot(inventory, SLOT_CHESTPLATE, x(1), y(5)))
            addSlot(createBackedSlot(inventory, SLOT_LEGGINGS, x(2), y(5)))
            addSlot(createBackedSlot(inventory, SLOT_BOOTS, x(3), y(5)))
            // 49: 装饰
            addSlot(FillerPaneSlot(x(4), y(5)))
            // 50: 副手
            addSlot(createBackedSlot(inventory, SLOT_OFFHAND, x(5), y(5)))
            // 50-53: 装饰占位 (伪槽位)
            for (col in 6 until 9) {
                addSlot(FillerPaneSlot(x(col), y(5)))
            }
            // 底部 36 槽: 观察者自己的背包, 保持原生行为
            val pad = (6 - 4) * 18
            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    addSlot(Slot(viewer.inventory, row * 9 + col + 9, x(col), pad + row * 18 + 103))
                }
            }
            for (col in 0 until 9) {
                addSlot(Slot(viewer.inventory, col, x(col), pad + 161))
            }
        }


        protected open fun createBackedSlot(inventory: NMSInventory0, containerIndex: Int, x: Int, y: Int): Slot {
            if (readOnly) return ReadOnlySlot(inventory, containerIndex, x, y)
            return Slot(inventory, containerIndex, x, y)
        }

        protected open fun intercept(slotId: Int, quickCraft: Boolean): Boolean {
            when {
                // 快捷栏选择器: 切换目标手持槽
                slotId in SELECTOR_START..SELECTOR_END -> {
                    if (!readOnly) {
                        switchHeldSlot(slotId - SELECTOR_START)
                    }
                    return true
                }
                // 装饰占位
                slotId in FILLER_START until TOP_SIZE -> return true
                // 只读模式下顶部全部吞掉; 拖拽时强制全量同步消除客户端幽灵物品
                readOnly && slotId in 0 until TOP_SIZE -> {
                    if (quickCraft) {
                        sendAllDataToRemote()
                    }
                    return true
                }
                else -> return false
            }
        }

        protected open fun switchHeldSlot(index: Int) {
            val bukkitTarget = whom.bukkitEntity
            if (bukkitTarget.inventory.heldItemSlot != index) {
                bukkitTarget.inventory.heldItemSlot = index
                val bukkitViewer = viewer.bukkitEntity
                bukkitViewer.playSound(bukkitViewer.location, Sound.UI_BUTTON_CLICK, 0.3f, 1.2f)
            }
        }

        //endregion

        //region Shift 转移: 顶部 -> 观察者背包, 底部 -> 目标背包 (伪槽位 mayPlace=false 自动跳过)

        override fun quickMoveStack(player: NMSPlayer0, index: Int): ItemStack {
            val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
            if (!slot.hasItem()) return ItemStack.EMPTY
            val itemStack = slot.item
            val original = itemStack.copy()
            val moved = if (index < TOP_SIZE) {
                moveItemStackTo(itemStack, TOP_SIZE, slots.size, true)
            } else {
                moveItemStackTo(itemStack, 0, TOP_SIZE, false)
            }
            if (!moved) return ItemStack.EMPTY
            if (itemStack.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
            return original
        }

        //暴露接口
        override fun stillValid(player: NMSPlayer0): Boolean = true
        override fun getBukkitView(): CraftInventoryView<*, *> {
            return bukkitView ?: run {
                // 仅用于 Bukkit 事件/视图元数据, 真实数据走 NMS 槽位
                val top = Bukkit.createInventory(null, TOP_SIZE, title)
                CraftInventoryView(viewer.bukkitEntity, top, this).also { bukkitView = it }
            }
        }

        //只读槽: 展示物品但禁止修改
        protected open class ReadOnlySlot(container: Container, index: Int, x: Int, y: Int) : Slot(container, index, x, y) {
            override fun mayPlace(stack: ItemStack): Boolean = false
            override fun mayPickup(player: NMSPlayer0): Boolean = false
            override fun allowModification(player: NMSPlayer0): Boolean = false
            override fun isFake(): Boolean = true
            override fun isHighlightable(): Boolean = false
            override fun getMaxStackSize(): Int = 0
            override fun getMaxStackSize(stack: ItemStack): Int = 0
            override fun remove(amount: Int): ItemStack = ItemStack.EMPTY
            override fun set(stack: ItemStack) {}
            override fun setByPlayer(stack: ItemStack) {}
            override fun setByPlayer(newStack: ItemStack, oldStack: ItemStack) {}
            override fun setChanged() {}
            override fun onTake(player: NMSPlayer0, stack: ItemStack) {}
            override fun onQuickCraft(newStack: ItemStack, original: ItemStack) {}
            override fun tryRemove(amount: Int, maxAmount: Int, player: NMSPlayer0): Optional<ItemStack> = Optional.empty()
            override fun safeTake(amount: Int, maxAmount: Int, player: NMSPlayer0): ItemStack = ItemStack.EMPTY
            override fun safeInsert(stack: ItemStack): ItemStack = stack
            override fun safeInsert(stack: ItemStack, amount: Int): ItemStack = stack
        }

        //虚拟槽: 仅用于客户端展示
        protected abstract inner class FakeSlot(x: Int, y: Int) : Slot(whom.inventory, 0, x, y) {
            protected abstract fun display(): ItemStack
            override fun getItem(): ItemStack = display()
            override fun hasItem(): Boolean = true
            override fun mayPlace(stack: ItemStack): Boolean = false
            override fun mayPickup(player: NMSPlayer0): Boolean = false
            override fun allowModification(player: NMSPlayer0): Boolean = false
            override fun isFake(): Boolean = true
            override fun isHighlightable(): Boolean = false
            override fun getMaxStackSize(): Int = 0
            override fun getMaxStackSize(stack: ItemStack): Int = 0
            override fun remove(amount: Int): ItemStack = ItemStack.EMPTY
            override fun set(stack: ItemStack) {}
            override fun setByPlayer(stack: ItemStack) {}
            override fun setByPlayer(newStack: ItemStack, oldStack: ItemStack) {}
            override fun setChanged() {}
            override fun onTake(player: NMSPlayer0, stack: ItemStack) {}
            override fun onQuickCraft(newStack: ItemStack, original: ItemStack) {}
            override fun tryRemove(amount: Int, maxAmount: Int, player: NMSPlayer0): Optional<ItemStack> = Optional.empty()
            override fun safeTake(amount: Int, maxAmount: Int, player: NMSPlayer0): ItemStack = ItemStack.EMPTY
            override fun safeInsert(stack: ItemStack): ItemStack = stack
            override fun safeInsert(stack: ItemStack, amount: Int): ItemStack = stack
        }

        protected open inner class SelectorPaneSlot(x: Int, y: Int, private val hotbarIndex: Int) : FakeSlot(x, y) {
            private val paneSelected: ItemStack = CraftItemStack.asNMSCopy(BukkitItemStack(Material.LIME_STAINED_GLASS_PANE)).apply {
                    set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(true, ReferenceSortedSets.emptySet()))
                    set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putBoolean(CUSTOM_DATA_PANE_ACTIVE, true) } ))
            }
            override fun display(): ItemStack {
                val selected = whom.bukkitEntity.inventory.heldItemSlot
                return if (hotbarIndex == selected) paneSelected else PANE
            }
        }

        protected open inner class FillerPaneSlot(x: Int, y: Int) : FakeSlot(x, y) {
            override fun display(): ItemStack = PANE
        }
    }

}