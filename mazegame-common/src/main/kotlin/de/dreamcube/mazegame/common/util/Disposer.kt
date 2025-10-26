package de.dreamcube.mazegame.common.util

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Chat-GPT-inspired class to collect multiple disposable objects for avoiding cyclic dependencies.
 */
class Disposer : AutoCloseable {

    private val disposeActions: MutableList<() -> Unit> = LinkedList()
    private val closed = AtomicBoolean(false)

    fun addDisposeAction(disposeAction: () -> Unit) {
        if (closed.get()) return
        disposeActions.addFirst(disposeAction)
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }
        disposeActions.forEach { disposeAction ->
            runCatching { disposeAction() }
        }
        disposeActions.clear()
    }
}