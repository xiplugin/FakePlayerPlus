package com.coderxi.plugin.fakeplayer.api.action

import com.coderxi.plugin.fakeplayer.api.utils.Order
import java.util.concurrent.ConcurrentHashMap

sealed interface Action {
    val type: ActionType
    val track get() = type.track
    val supportModes : Collection<Class<out ActionMode>>
    val mode: ActionMode
    companion object {
        private val mapping = listOf(
            AttackAction::class.java,
            MineAction::class.java,
            UseItemAction::class.java,
            DropItemAction::class.java,
            JumpAction::class.java,
            SneakAction::class.java,
        ).associateBy {
            it.getDeclaredField("type").get(null) as ActionType
        }
        private val supportModesCache = ConcurrentHashMap<ActionType, List<Class<out ActionMode>>>()
        fun toClass(type: ActionType) = mapping[type]!!
        fun getSupportModes(type: ActionType) = supportModesCache.computeIfAbsent(type) { type ->
            mapping[type]?.declaredConstructors
                ?.mapNotNull { it.parameterTypes.firstOrNull() }
                ?.filter { it != ActionMode::class.java && ActionMode::class.java.isAssignableFrom(it) }
                ?.map { @Suppress("UNCHECKED_CAST") (it as Class<out ActionMode>) }
                ?.sortedBy { it.getAnnotation(Order::class.java).value }
                ?:emptyList()
        }
    }
    abstract class Base(
        override val mode: ActionMode
    ) : Action {
        override val type by lazy { this.javaClass.getDeclaredField("type").get(null) as ActionType }
        override val supportModes by lazy { getSupportModes(type) }
    }
}