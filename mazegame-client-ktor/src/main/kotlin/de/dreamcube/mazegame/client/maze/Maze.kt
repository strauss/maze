package de.dreamcube.mazegame.client.maze

import java.util.*

/**
 * Space-efficient client-side representation of the maze. The maze is represented as two 1D BitSets. The constructor requires the [width] and the
 * [height] of the maze and the [lines] from the server. The [lines] are parsed. The number of lines have to match the height and the length of each
 * String has to match the [with]. Otherwise, the [Maze] is not created.
 */
class Maze(val width: Int, val height: Int, lines: List<String>) {

    companion object {
        /**
         * Enum class representing a maze value.
         */
        enum class FieldValue(internal val left: Boolean, internal val right: Boolean, val asChar: Char) {
            /**
             * This value is used for outside fields. It can also be used "inside" for artistic reasons and in non-rectangular mazes. These fields are
             * not walkable.
             */
            OUTSIDE(false, false, '-'),

            /**
             * This value is used for walls inside the maze. These fields are not walkable.
             */
            WALL(false, true, '#'),

            /**
             * This value is used for pathways inside the maze. These fields are walkable.
             */
            PATH(true, false, '.'),

            /**
             * This value is used if the server sends gibberish (or question marks). These fields should not occur. If they do, they are not walkable.
             */
            UNKNOWN(true, true, '?');

            companion object {

                @JvmStatic
                internal fun getByBits(left: Boolean, right: Boolean): FieldValue = when {
                    !left && !right -> OUTSIDE
                    !left && right -> WALL
                    left && !right -> PATH
                    else -> UNKNOWN
                }

                @JvmStatic
                internal fun getByChar(char: Char): FieldValue = when (char) {
                    OUTSIDE.asChar -> OUTSIDE
                    WALL.asChar -> WALL
                    PATH.asChar -> PATH
                    else -> UNKNOWN
                }
            }
        }
    }

    /**
     * The left [BitSet] representing this [Maze].
     */
    private val leftSet = BitSet()

    /**
     * The right [BitSet] representing this [Maze].
     */
    private val rightSet = BitSet()

    init {
        check(width >= 1 && height >= 1) { "Maze dimensions too small." }
        var y = 0
        for (currentLine: String in lines) {
            check(currentLine.length == width) { "Maze line should have length of $width but has length ${currentLine.length}." }
            var x = 0
            for (c: Char in currentLine) {
                this[x, y] = FieldValue.getByChar(c)
                x += 1
            }
            y += 1
        }
        check(y == height) { "Received $y maze lines but expected $height." }
    }

    private operator fun set(x: Int, y: Int, value: FieldValue) {
        val position: Int = y * width + x
        leftSet[position] = value.left
        rightSet[position] = value.right
    }

    /**
     * Retrieves the [FieldValue] at the given position.
     */
    operator fun get(x: Int, y: Int): FieldValue {
        val position: Int = y * width + x
        return FieldValue.getByBits(leftSet[position], rightSet[position])
    }
}