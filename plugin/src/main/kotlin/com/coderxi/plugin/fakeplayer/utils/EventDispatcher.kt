package com.coderxi.plugin.fakeplayer.utils

interface EventDispatcher<E : Any> {
    val eventBus: EventBus
    fun emit(event: E) {
        eventBus.emit(event)
    }

    val on: Listen get() = Listen(eventBus)
    fun on(classSimpleName: String,priority: Int = 0, action: (Any)-> Unit) = eventBus.registerEvent(classSimpleName, priority,action)

    class Listen(val bus: EventBus) {
        inline operator fun <reified E : Any> invoke(
            priority: Int = 0,
            noinline action: (E) -> Unit
        ) {
            val name = E::class.simpleName ?: return
            bus.registerEvent(name, priority) { event ->
                if (event is E) action(event)
            }
        }
    }
}