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
private fun combineIntsToLong(x: Int, y: Int): Long = x.toLong().shl(32) or (y.toLong() and 0xFFFF_FFFFL)
