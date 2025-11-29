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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A collection of [Bait]s.
 */
class BaitCollection {
    /**
     * [kotlinx.coroutines.sync.Mutex] for thread- ... coroutine-safety
     */
    private val baitMutex = Mutex()

    /**
     * The internal structure holding all the [Bait]s. The [MutableMap] is used for performance reasons.
     */
    private val positionIdToBaitMap: MutableMap<Long, Bait> = LinkedHashMap()

    /**
     * Adds a new [Bait] to this [BaitCollection]. If the desired position already contains a bait, nothing happens. Returns true, if something was
     * actually added.
     */
    internal suspend fun addBait(bait: Bait): Boolean {
        baitMutex.withLock {
            val baitId = bait.id
            if (!positionIdToBaitMap.containsKey(baitId)) {
                positionIdToBaitMap.put(baitId, bait)
                return true
            }
            return false
        }
    }

    /**
     * Removes a [Bait] from this [BaitCollection]. Returns true, if something was actually removed.
     */
    internal suspend fun removeBait(bait: Bait): Boolean {
        baitMutex.withLock {
            return positionIdToBaitMap.remove(bait.id) != null
        }
    }

    /**
     * Retrieves the [Bait] at the given coordinates. Returns null if there is none.
     */
    internal suspend fun getBait(x: Int, y: Int): Bait? {
        baitMutex.withLock {
            return positionIdToBaitMap[Bait.toId(x, y)]
        }
    }

    /**
     * Returns an immutable [List] of all [Bait]s that are currently contained in this [BaitCollection]. The [List] is a copy of the value collection
     * of the internal map. If the map changes, the copy will therefore not reflect these changes.
     */
    internal suspend fun elements(): List<Bait> {
        baitMutex.withLock {
            return positionIdToBaitMap.values.toList()
        }
    }

}