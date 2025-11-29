/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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