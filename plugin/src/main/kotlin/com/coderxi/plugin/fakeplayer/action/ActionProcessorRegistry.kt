package com.coderxi.plugin.fakeplayer.action

import com.coderxi.plugin.fakeplayer.action.processor.*
import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionType

object ActionProcessorRegistry {

    private val registry: Map<ActionType, ActionProcessor<*>> = mapOf(
        ActionType.ATTACK to AttackProcessor,
        ActionType.MINE to MineProcessor,
        ActionType.USE_ITEM to UseItemProcessor,
        ActionType.DROP_ITEM to DropItemProcessor,
        ActionType.JUMP to JumpProcessor,
        ActionType.SNEAK to SneakProcessor,
        ActionType.FLATTEN to FlattenProcessor
    )

    @Suppress("UNCHECKED_CAST")
    fun <T : Action> get(action: T): ActionProcessor<T>? {
        return registry[action.type] as? ActionProcessor<T>
    }

}