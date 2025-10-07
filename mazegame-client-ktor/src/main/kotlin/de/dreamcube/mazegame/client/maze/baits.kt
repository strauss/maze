package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.BaitType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple data class representing a [Bait] of a specific [BaitType] at a unique position represented by [x] and [y].
 */
data class Bait(var type: BaitType, val x: Int, val y: Int) {

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
        internal fun toId(x: Int, y: Int): Long = x.toLong().shl(32) or (y.toLong() and 0xFFFF_FFFFL)
    }
}

/**
 * A collection of [Bait]s.
 */
class BaitCollection {
    /**
     * [Mutex] for thread- ... coroutine-safety
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