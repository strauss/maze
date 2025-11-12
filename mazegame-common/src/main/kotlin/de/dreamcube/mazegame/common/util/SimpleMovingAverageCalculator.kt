package de.dreamcube.mazegame.common.util

import java.util.*
import kotlin.math.roundToLong

/**
 * Utility class for calculating a moving average. Is used by the server for the delay compensation.
 */
class SimpleMovingAverageCalculator(windowSize: Int) : AverageCalculator<Long> {
    val values: LongArray = LongArray(windowSize)
    var internalSum: Long = 0L
    override val sum: Long
        get() = internalSum

    var filled: Int = 0
    override val numberOfRelevantElements: Int
        get() = filled

    var next: Int = 0

    override fun addValue(value: Long) {
        if (filled < values.size) {
            values[next] = value
            filled += 1
        } else {
            internalSum -= values[next]
            values[next] = value
        }
        next = (next + 1) % values.size
        internalSum += value
    }

    override val average: Long
        get() = if (filled <= 0) 0L else (sum.toDouble() / filled.toDouble()).roundToLong()

    override fun reset() {
        Arrays.fill(values, 0L)
        internalSum = 0L
        filled = 0
        next = 0
    }
}