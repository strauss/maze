package de.dreamcube.mazegame.common.maze

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.path.Path
import kotlin.io.path.readText

/**
 * Space-efficient representation of the maze. The maze is represented as two 1D BitSets. The constructor requires the
 * [width] and the [height] of the maze. It is not intended for "good" bots. The inferior a-star strategies, that are
 * contained in the default client/server implementation, use this maze representation.
 *
 * This structure is also used for the compact maze representation that is sent to the client when requesting meta
 * information. The UI can use it to render a minimap as preview.
 */
class CompactMaze(val width: Int, val height: Int) {

    companion object {
        /**
         * This factory function requires [width], [height], and the [lines] from the server. The [lines] are parsed.
         * The number of lines have to match the height and the length of each String has to match the [with].
         * Otherwise, the [CompactMaze] is not created.
         */
        @JvmStatic
        fun createFromLines(width: Int, height: Int, lines: List<String>): CompactMaze {
            check(width >= 1 && height >= 1) { "Maze dimensions too small." }
            val maze = CompactMaze(width, height)
            var y = 0
            for (currentLine: String in lines) {
                check(currentLine.length == width) { "Maze line should have length of $width but has length ${currentLine.length}." }
                var x = 0
                for (c: Char in currentLine) {
                    maze[x, y] = FieldValue.getByChar(c)
                    x += 1
                }
                y += 1
            }
            check(y == height) { "Received $y maze lines but expected $height." }
            return maze
        }

        /**
         * Creates a new [CompactMaze] from the two given [java.util.BitSet]s [leftSet] and [rightSet]. Their content is copied
         * and their combination, together with the given dimensions, are interpreted as maze data, when accessed.
         */
        @JvmStatic
        fun createFromBitSets(width: Int, height: Int, leftSet: BitSet, rightSet: BitSet): CompactMaze {
            check(width >= 1 && height >= 1) { "Maze dimensions too small." }
            val maze = CompactMaze(width, height)
            // The internal bit sets are all false inside.
            // By "or"ing them, we just copy whatever is in the given two bit sets
            maze.leftSet.or(leftSet)
            maze.rightSet.or(rightSet)
            return maze
        }

        /**
         * Imports the [CompactMaze] from a byte array.
         */
        fun import(rawBytes: ByteArray): CompactMaze {
            val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
            val width = buffer.getInt()
            val height = buffer.getInt()
            val leftSize = buffer.getInt()
            val leftArray = ByteArray(leftSize)
            buffer[leftArray]
            val rightSize = buffer.getInt()
            val rightArray = ByteArray(rightSize)
            buffer[rightArray]
            val leftSet = BitSet.valueOf(leftArray)
            val rightSet = BitSet.valueOf(rightArray)
            return createFromBitSets(width, height, leftSet, rightSet)
        }
    }

    /**
     * The left [BitSet] representing this [CompactMaze].
     */
    private val leftSet = BitSet()

    /**
     * The right [BitSet] representing this [CompactMaze].
     */
    private val rightSet = BitSet()

    /**
     * Sets the [value] to the given coordinates. Out of bounds will be ignored.
     */
    operator fun set(x: Int, y: Int, value: FieldValue) {
        if (x !in 0..<width || y !in 0..<height) {
            return
        }
        val position: Int = y * width + x
        leftSet[position] = value.left
        rightSet[position] = value.right
    }

    /**
     * Retrieves the [FieldValue] at the given position. This cannot go out of bounds. If [x] or [y] are given out
     * of bounds, [FieldValue.OUTSIDE] is returned, as it is logically the correct value for this case.
     */
    operator fun get(x: Int, y: Int): FieldValue {
        // bit sets can't handle negative numbers, so we have to be explicit here
        if (x < 0 || y < 0) {
            return FieldValue.OUTSIDE
        }
        val position: Int = y * width + x
        // bit sets are only limited by Int.MAX_VALUE. Therefore, we don't have to be explicit here.
        // (false, false) maps to outside, which is what we want here.
        return FieldValue.getByBits(leftSet[position], rightSet[position])
    }

    fun export(): ByteArray {
        val leftAsArray = leftSet.toByteArray()
        val rightAsArray = rightSet.toByteArray()
        // we store width, height, length of left, left, length of right, right
        // therefore we need four ints (16 bytes) and the lengths of both byte arrays
        val result = ByteBuffer.allocate(16 + leftAsArray.size + rightAsArray.size).order(ByteOrder.LITTLE_ENDIAN)
        result.putInt(width)
        result.putInt(height)
        result.putInt(leftAsArray.size)
        result.put(leftAsArray)
        result.putInt(rightAsArray.size)
        result.put(rightAsArray)
        return result.array()
    }

    override fun toString(): String = buildString {
        for (y in 0..<height) {
            for (x in 0..<width) {
                append(this@CompactMaze[x, y].asChar)
            }
            if (y < height - 1) {
                append(System.lineSeparator())
            }
        }
    }

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

fun main() {
    val mazeLines: List<String> =
        Path("./mazegame-server-ktor/src/main/resources/de/dreamcube/mazegame/server/maze/generator/square.mzt").readText()
            .trim().lines()
    val maze = CompactMaze.createFromLines(mazeLines.first().length, mazeLines.size, mazeLines)
    val asString = maze.toString()
    println(asString)
    println(asString.length)
    val export = maze.export()
    val encoded = Base64.encode(export)
    println(encoded)
    println(encoded.length)
    val importedMaze = CompactMaze.import(export)
    println(importedMaze)
}