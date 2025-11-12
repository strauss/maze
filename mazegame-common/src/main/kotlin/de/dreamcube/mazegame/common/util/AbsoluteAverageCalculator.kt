package de.dreamcube.mazegame.common.util

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToLong

/**
 * Utility class to calculate an absolute average.
 */
class AbsoluteAverageCalculator : AverageCalculator<Long> {

    private val internalSum = AtomicLong(0L)
    override val sum
        get() = internalSum.get()

    private val counter = AtomicInteger(0)

    override fun addValue(value: Long) {
        Objects.requireNonNull<Long?>(value)
        internalSum.addAndGet(value)
        counter.incrementAndGet()
    }

    override val average: Long
        get() {
            val localSum = internalSum.get()
            val localCounter = counter.get()
            if (localCounter == 0) {
                return 0L
            }
            return (localSum / localCounter.toDouble()).roundToLong()
        }

    override val numberOfRelevantElements: Int
        get() = counter.get()

    override fun reset() {
        internalSum.set(0L)
        counter.set(0)
    }
}