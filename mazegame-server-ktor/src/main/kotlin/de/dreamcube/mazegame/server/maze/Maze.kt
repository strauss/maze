package de.dreamcube.mazegame.server.maze

import de.dreamcube.mazegame.common.maze.CompactMaze
import de.dreamcube.mazegame.common.maze.ViewDirection
import de.dreamcube.mazegame.server.maze.Maze.Companion.PATH
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.math.max

class Maze(val width: Int, val height: Int) {

    /**
     * Internal representation of the maze as 1D-Array.
     * TODO: Make it hornet queen
     */
    private val maze: ByteArray = ByteArray(width * height)

    /**
     * Bit set representing the occupied status of walkable fields.
     */
    private val occupied: BitSet = BitSet()

    private val occupiedMutex: Mutex = Mutex()

    operator fun get(x: Int, y: Int): Byte = maze[y * width + x]

    operator fun set(x: Int, y: Int, value: Byte) {
        maze[y * width + x] = value
    }

    suspend fun isOccupied(x: Int, y: Int): Boolean {
        return occupiedMutex.withLock { occupied[y * width + x] }
    }

    suspend fun occupyPosition(x: Int, y: Int) {
        occupiedMutex.withLock { internalOccupyPosition(x, y) }
    }

    suspend fun move(fromX: Int, fromY: Int, toX: Int, toY: Int) {
        occupiedMutex.withLock {
            internalReleasePosition(fromX, fromY)
            internalOccupyPosition(toX, toY)
        }
    }

    suspend fun releasePosition(x: Int, y: Int) {
        occupiedMutex.withLock { internalReleasePosition(x, y) }
    }

    private fun internalOccupyPosition(x: Int, y: Int) {
        occupied[y * width + x] = true
    }

    private fun internalReleasePosition(x: Int, y: Int) {
        occupied[y * width + x] = false
    }

    /**
     * Fills a rectangular area of the maze with a specified marker.
     *
     * @param x X coordinate of top left corner
     * @param y Y coordinate of top left corner
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param value marker value
     */
    fun fill(x: Int, y: Int, width: Int, height: Int, value: Byte) {
        for (j in y..<y + height) {
            for (i in x..<x + width) {
                this[i, j] = value
            }
        }
    }

    /**
     * Frames a rectangular area of the maze with a specified marker.
     *
     * @param x X coordinate of top left corner
     * @param y Y coordinate of top left corner
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param value marker value
     */
    fun frame(x: Int, y: Int, width: Int, height: Int, value: Byte) {
        fill(x, y, width, 1, value)
        fill(x, y + 1, 1, height - 2, value)
        fill(x + width - 1, y + 1, 1, height - 2, value)
        fill(x, y + height - 1, width, 1, value)
    }

    fun getNumberOfWalkableNeighbors(x: Int, y: Int): Int {
        val currentCell: Byte = this[x, y]
        if (currentCell != PATH) {
            return -1
        }
        var result = 0
        if (getNorthOf(x, y) == PATH) {
            result += 1
        }
        if (getEastOf(x, y) == PATH) {
            result += 1
        }
        if (getSouthOf(x, y) == PATH) {
            result += 1
        }
        if (getWestOf(x, y) == PATH) {
            result += 1
        }
        return result
    }

    private fun getNorthOf(x: Int, y: Int): Byte {
        if (y > 0) {
            return this[x, y - 1]
        }
        return OUTSIDE
    }

    private fun getEastOf(x: Int, y: Int): Byte {
        if (x < width - 1) {
            return this[x + 1, y]
        }
        return OUTSIDE
    }

    private fun getSouthOf(x: Int, y: Int): Byte {
        if (y < height - 1) {
            return this[x, y + 1]
        }
        return OUTSIDE
    }

    private fun getWestOf(x: Int, y: Int): Byte {
        if (x > 0) {
            return this[x - 1, y]
        }
        return OUTSIDE
    }

    /**
     * Returns true, if one step in [direction] is walkable ([PATH]). Implicitly performs a boundary check if a maniac created a map without outer
     * walls and another maniac tried to step outside.
     */
    fun isWalkable(x: Int, y: Int, direction: ViewDirection): Boolean {
        return PATH == when (direction) {
            ViewDirection.NORTH -> getNorthOf(x, y)
            ViewDirection.EAST -> getEastOf(x, y)
            ViewDirection.SOUTH -> getSouthOf(x, y)
            ViewDirection.WEST -> getWestOf(x, y)
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (y in 0..<height) {
            for (x in 0..<width) {
                val c = when (this[x, y]) {
                    OUTSIDE -> '-'
                    WALL -> '#'
                    PATH -> '.'
                    else -> '?'
                }
                sb.append(c)
            }
            if (y < height - 1) {
                sb.append('\n')
            }
        }
        return sb.toString()
    }

    /**
     * Creates a compact representation of this [Maze]'s data.
     */
    internal fun toCompactMaze(): CompactMaze {
        val result = CompactMaze(width, height)
        for (y in 0..<height) {
            for (x in 0..<width) {
                result[x, y] = when (this[x, y]) {
                    OUTSIDE -> CompactMaze.FieldValue.OUTSIDE
                    WALL -> CompactMaze.FieldValue.WALL
                    PATH -> CompactMaze.FieldValue.PATH
                    else -> CompactMaze.FieldValue.UNKNOWN
                }
            }
        }
        return result
    }

    companion object {
        const val OUTSIDE: Byte = 0
        const val WALL: Byte = 1
        const val PATH: Byte = 2
        const val POSSIBLE: Byte = 3
        const val UNKNOWN: Byte = 127

        fun fromLines(lines: List<String>): Maze {
            // pre-process
            val height: Int = lines.size
            var width = 0
            lines.forEach { line -> width = max(width, line.length) }

            // Initialize with all outside (zero is default value)
            val maze = Maze(width, height)

            // fill the maze with data from string representation
            var y = 0
            for (line: String in lines) {
                for (x in 0..line.lastIndex) {
                    maze[x, y] = when (line[x]) {
                        '-' -> OUTSIDE
                        '#' -> WALL
                        '.' -> PATH
                        '?' -> POSSIBLE
                        else -> UNKNOWN
                    }
                }
                y += 1
            }

            return maze
        }
    }


}