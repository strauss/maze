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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToLong

/**
 * Utility class to calculate an absolute average.
 */
@Suppress("unused")
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