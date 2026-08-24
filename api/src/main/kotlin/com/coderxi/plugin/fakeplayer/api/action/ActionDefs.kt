package com.coderxi.plugin.fakeplayer.api.action

import com.coderxi.plugin.fakeplayer.api.action.ActionMode.*
import com.coderxi.plugin.fakeplayer.api.action.ActionType.*
import org.bukkit.block.Block

class AttackAction private constructor(mode: ActionMode): Action.Base(mode) {
    companion object { @JvmField val type = ATTACK }
    constructor(mode: Once) : this(mode as ActionMode)
    constructor(mode: Interval) : this(mode as ActionMode)
}

class MineAction private constructor(override val mode: ActionMode): Action.Base(mode) {
    companion object { @JvmField val type = MINE }
    constructor(mode: Continuous) : this(mode as ActionMode)
    var target: Block? = null
    var progress = 0f
    var freezeTick = 0
}

class UseItemAction private constructor(mode: ActionMode): Action.Base(mode) {
    companion object { @JvmField val type = USE_ITEM }
    constructor(mode: Once) : this(mode as ActionMode)
    constructor(mode: Interval) : this(mode as ActionMode)
    constructor(mode: Continuous) : this(mode as ActionMode)
    var freezeTick = 0
}

class DropItemAction private constructor(mode: ActionMode): Action.Base(mode) {
    companion object { @JvmField val type = DROP_ITEM }
    constructor(mode: Once) : this(mode as ActionMode)
    constructor(mode: Interval) : this(mode as ActionMode)
}

class JumpAction private constructor(mode: ActionMode): Action.Base(mode) {
    companion object { @JvmField val type = JUMP }
    constructor(mode: Once) : this(mode as ActionMode)
    constructor(mode: Interval) : this(mode as ActionMode)
}

class SneakAction private constructor(mode: ActionMode): Action.Base(mode) {
    companion object { @JvmField val type = SNEAK }
    constructor(mode: Once) : this(mode as ActionMode)
    constructor(mode: Continuous) : this(mode as ActionMode)
}

class FlattenAction private constructor(override val mode: ActionMode): Action.Base(mode) {
    companion object { @JvmField val type = FLATTEN }
    constructor(mode: Continuous) : this(mode as ActionMode)
    var world: org.bukkit.World? = null
    var minX: Int = 0
    var minY: Int = 0
    var minZ: Int = 0
    var maxX: Int = 0
    var maxY: Int = 0
    var maxZ: Int = 0
    var target: Block? = null
    var lastTargetName: String = "-"
    var progress = 0f
    var freezeTick = 0
    var preserveOres: Boolean = false
    var pickupItems: Boolean = true
    var tickDelay: Int = 2
    var totalBlocks: Int = 0
    var clearedBlocks: Int = 0
    var stuckTick: Int = 0
    var lastLoc: org.bukkit.Location? = null
    var chestWorld: org.bukkit.World? = null
    var chestX: Int? = null
    var chestY: Int? = null
    var chestZ: Int? = null
    var chestLocations: MutableList<org.bukkit.Location> = mutableListOf()
    var autoDeposit: Boolean = true
    var isDepositing: Boolean = false
}