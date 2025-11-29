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

package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.BaitType

/**
 * Simple data class representing a [Bait] of a specific [BaitType] at a unique position represented by [x] and [y].
 */
data class Bait(val type: BaitType, val x: Int, val y: Int) {

    /**
     * The [id] of the [Bait] (used for internal storage).
     */
    internal val id
        get() = toId(x, y)

    /**
     * The [score] of the [Bait] determined by its [type]. The value is directly taken from the [BaitType] enum.
     */
    val score
        get() = type.score

    companion object {
        /**
         * Internal function for turning the [x] and [y] coordinates into an internal [id].
         */
        internal fun toId(x: Int, y: Int): Long = combineIntsToLong(x, y)
    }
}

/**
 * Function for turning the [x] and [y] coordinates into a long number. [x] is placed in the higher order bits and [y]
 * is placed in the lower order bits. Signs are truncated and therefore ignored. The bit values are taken "as is".
 */
fun combineIntsToLong(x: Int, y: Int): Long = x.toLong().shl(32) or (y.toLong() and 0xFFFF_FFFFL)
