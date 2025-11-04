package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.MazeEventListener
import de.dreamcube.mazegame.client.maze.events.PlayerMovementListener
import de.dreamcube.mazegame.client.maze.strategy.maze_representations.Maze
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType
import de.dreamcube.mazegame.common.maze.ViewDirection
import java.util.*
import kotlin.math.abs

/**
 * AStar search for reaching a preselected target bait. A very bad foundation for "good" bots, but suitable for trapeaters and other shenanigans.
 */
abstract class SingleTargetAStar : Strategy(), MazeEventListener, PlayerMovementListener {
    /**
     * Internal representation of the maze.
     */
    protected lateinit var maze: Maze

    /**
     * The selected target trap.
     */
    protected var currentTarget: Bait? = null

    /**
     * The move list towards the target.
     */
    protected val path: MutableList<PlayerPosition> = ArrayList()

    override fun getNextMove(): Move {
        if (currentTarget == null) {
            return Move.DO_NOTHING
        }
        if (path.isNotEmpty()) {
            return extractNextMoveFromPath()
        }

        // we have a target, but no path yet. A* baby!
        return aStarSearch()
    }

    private fun aStarSearch(): Move {
        val marker = HashSet<PlayerPosition>()
        val queue = PriorityQueue<SearchState>(compareBy { it.costSoFar + it.estimatedCost })
        val costs = HashMap<PlayerPosition, Int>()
        val parent = HashMap<PlayerPosition, PlayerPosition>()

        val myPosition = mazeClient.ownPlayerSnapshot.position
        costs[myPosition] = 0
        val target = currentTarget!!
        val estimatedCost = getManhattanDistance(myPosition.x, target.x, myPosition.y, target.y)
        queue.offer(SearchState(myPosition, 0, estimatedCost))
        var targetPosition: PlayerPosition? = null
        while (queue.isNotEmpty()) {
            val currentState = queue.poll()
            val currentPosition = currentState.position
            // check if we've been here already
            if (currentPosition in marker) {
                continue
            }
            marker.add(currentPosition)
            // check if we're there
            if (target.x == currentPosition.x && target.y == currentPosition.y) {
                targetPosition = currentPosition
                break
            }
            // follow-up states
            val followUpPositions =
                listOf(currentPosition.whenRight(), currentPosition.whenLeft(), currentPosition.whenStep())
            for (nextPosition in followUpPositions) {
                if (!walkable(nextPosition)) continue

                val newCost = currentState.costSoFar + 1

                if ((costs[nextPosition] ?: Integer.MAX_VALUE) > newCost) {
                    parent.put(nextPosition, currentPosition)
                    costs[nextPosition] = newCost
                    val nextEstimate = getManhattanDistance(nextPosition.x, target.x, nextPosition.y, target.y)
                    val nextState = SearchState(nextPosition, newCost, nextEstimate)
                    queue.offer(nextState)
                }
            }
        }
        if (targetPosition == null) {
            return Move.DO_NOTHING
        }
        var currentPosition: PlayerPosition? = targetPosition
        while (currentPosition != null) {
            path.add(currentPosition)
            currentPosition = parent[currentPosition]
        }
        return extractNextMoveFromPath()
    }

    protected fun extractNextMoveFromPath(): Move {
        val lastPosition = path.removeLast()
        if (path.isEmpty()) {
            currentTarget = null
            return Move.DO_NOTHING
        }
        return getMove(lastPosition, path.last())
    }

    private fun getMove(source: PlayerPosition, target: PlayerPosition): Move {
        val deltaX = target.x - source.x
        val deltaY = target.y - source.y
        if (source.viewDirection == target.viewDirection && (deltaX != 0 || deltaY != 0)) {
            return Move.STEP
        }
        if (source.viewDirection == ViewDirection.NORTH && target.viewDirection == ViewDirection.EAST ||
            source.viewDirection == ViewDirection.EAST && target.viewDirection == ViewDirection.SOUTH ||
            source.viewDirection == ViewDirection.SOUTH && target.viewDirection == ViewDirection.WEST ||
            source.viewDirection == ViewDirection.WEST && target.viewDirection == ViewDirection.NORTH
        ) {
            return Move.TURN_R
        }
        if (source.viewDirection == ViewDirection.NORTH && target.viewDirection == ViewDirection.WEST ||
            source.viewDirection == ViewDirection.WEST && target.viewDirection == ViewDirection.SOUTH ||
            source.viewDirection == ViewDirection.SOUTH && target.viewDirection == ViewDirection.EAST ||
            source.viewDirection == ViewDirection.EAST && target.viewDirection == ViewDirection.NORTH
        ) {
            return Move.TURN_L
        }
        return Move.DO_NOTHING
    }

    private fun walkable(position: PlayerPosition): Boolean =
        position.x >= 0 && position.y >= 0 && maze[position.x, position.y] == Maze.FieldValue.PATH

    private data class SearchState(val position: PlayerPosition, val costSoFar: Int, val estimatedCost: Int)

    protected fun getManhattanDistance(x1: Int, x2: Int, y1: Int, y2: Int): Int = abs(x1 - x2) + abs(y1 - y2)

    override fun onMazeReceived(width: Int, height: Int, mazeLines: List<String>) {
        maze = Maze(width, height, mazeLines)
    }

    override fun onPlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        // If we teleport, we drop the target and select a new one
        if (newPlayerSnapshot.id == mazeClient.id) {
            currentTarget = null
            path.clear()
        }
    }
}