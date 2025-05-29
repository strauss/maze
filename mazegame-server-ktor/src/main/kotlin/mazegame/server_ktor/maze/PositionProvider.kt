package mazegame.server_ktor.maze

import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

class PositionProvider(val maze: Maze) {
    private val walkablePositions: List<Position>
    private val rng: Random = Random()
    val walkablePositionsSize
        get() = walkablePositions.size

    init {
        walkablePositions = buildList {
            // collect all path elements and count their neighbors
            for (y in 0..<maze.height) {
                for (x in 0..<maze.width) {
                    val currentCell: Byte = maze[x, y]
                    if (currentCell == Maze.PATH) {
                        add(Position(x, y, maze.getNumberOfWalkableNeighbors(x, y)))
                    }
                }
            }

            // shuffle and sort by neighbor count
            shuffle(rng)
            sortWith(Comparator.comparing(Position::neighbors))
        }
    }

    /**
     * Determines a totally random free position in the maze.
     */
    suspend fun randomFreePosition(): Position {
        val randomListPosition: Int = rng.nextInt(walkablePositionsSize)
        return nextFreePosition(randomListPosition, true)
    }

    /**
     * Determines a random free position for teleporting a player preferring dead ends and hallways but only rarely junctions.
     */
    suspend fun randomPositionForTeleport(): Position {
        return randomGaussianFreePosition(0.2, false)
    }

    /**
     * Determines a random free position for generating a gem preferring dead ends and hallways but only very rarely junctions.
     */
    suspend fun randomPositionForGem(): Position {
        return randomGaussianFreePosition(0.15, false)
    }

    /**
     * Determines a random free position for traps, preferring junctions. Rarely hallways, almost never dead ends.
     */
    suspend fun randomPositionForTrap(): Position {
        return randomGaussianFreePosition(0.3, true)
    }

    /**
     * Determines a normally distributed random free position in the maze. Positions with dead ends are preferred.
     *
     * @param sigma    the standard derivation, should be < 0.5 for best results
     * @param inverted if true, junctions with more neighbors are preferred
     */
    private suspend fun randomGaussianFreePosition(sigma: Double, inverted: Boolean): Position {
        val gaussian = abs(rng.nextGaussian())
        var randomListPosition: Int = round(floor(gaussian * walkablePositionsSize * sigma)).toInt()
        randomListPosition %= walkablePositionsSize
        if (inverted) {
            randomListPosition = walkablePositionsSize - 1 - randomListPosition
        }
        return nextFreePosition(randomListPosition, !inverted)
    }

    private suspend fun nextFreePosition(startIndex: Int, forward: Boolean): Position {
        var result: Position = walkablePositions[startIndex]
        val delta: Int = if (forward) 1 else -1
        var i = delta
        while (maze.isOccupied(result.x, result.y)) {
            result = walkablePositions[abs(startIndex + i) % walkablePositionsSize]
            i += delta
        }
        return result
    }
}

data class Position(val x: Int, val y: Int, val neighbors: Int) {
    companion object {
        val spectatorPosition = Position(-1, -1, 0)
    }
}
