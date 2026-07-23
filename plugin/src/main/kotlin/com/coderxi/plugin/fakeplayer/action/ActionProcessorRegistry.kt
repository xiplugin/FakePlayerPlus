package com.coderxi.plugin.fakeplayer.action

import com.coderxi.plugin.fakeplayer.action.processor.*
import com.coderxi.plugin.fakeplayer.api.action.Action
import com.coderxi.plugin.fakeplayer.api.action.ActionType

object ActionProcessorRegistry {

    private val processors = listOf(
        AttackProcessor,
        MineProcessor,
        UseItemProcessor,
        DropItemProcessor,
        JumpProcessor,
        SneakProcessor
    )

    private val registry = processors.associateBy { it.actionType.getDeclaredField("type").apply { isAccessible = true }.get(null) as ActionType }

    @Suppress("UNCHECKED_CAST")
    fun <T : Action> get(action: T): ActionProcessor<T>? {
        return registry[action.type] as? ActionProcessor<T>
    }

}