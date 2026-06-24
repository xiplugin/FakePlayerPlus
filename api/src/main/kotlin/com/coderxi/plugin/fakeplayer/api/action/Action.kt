package com.coderxi.plugin.fakeplayer.api.action

import com.coderxi.plugin.fakeplayer.api.action.ActionTrigger.*
import org.bukkit.block.Block

object AttackOnce : AttackAction, Once
data class AttackInterval(override val intervalTicks: Int) : AttackAction, Interval

class MineContinuous : MineAction, Continuous { var target: Block? = null; var progress: Float = 0f; override var freezeTick: Int = 0 }

object UseItemOnce : UseItemAction, Once { override var freezeTick = 0 }
object UseItemContinuous : UseItemAction, Continuous { override var freezeTick = 0 }
data class UseItemInterval(override val intervalTicks: Int) : UseItemAction, Interval { override var freezeTick = 0 }

object JumpOnce : JumpAction, Once
object JumpContinuous : JumpAction, Continuous
data class JumpInterval(override val intervalTicks: Int) : JumpAction, Interval

object SneakOnce : SneakAction, Interval { override val intervalTicks get() = 1 }
object SneakContinuous : SneakAction, Continuous