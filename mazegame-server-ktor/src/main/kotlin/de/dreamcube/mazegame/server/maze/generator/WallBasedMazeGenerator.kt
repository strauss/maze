package de.dreamcube.mazegame.server.maze.generator

import de.dreamcube.mazegame.server.maze.Maze
import java.awt.Point
import kotlin.random.Random

/**
 * <p>
 * Kotlin port of the maze generator from the original maze server by Volker Riediger.
 * </p>
 * <p>
 * The original maze generation algorithm used to be a Java
 * port of a C program written by Christian Ernst Rysgaard (who copied it from an unknown source...). Unfortunately the link to the C program is dead
 * now (http://www.glimt.dk/code/labyrinth.htm (May 2003)).
 * </p>
 * <p>
 * This version allows for generating a random maze into a predefined template map. This now really allows for non-rectangular random mazes. The
 * buffer structure for maintaining the remaining possible wall positions was replaced with a random buffer. Also, the necessity to remove elements
 * from an array list was eliminated.
 * </p>
 */
class WallBasedMazeGenerator(val templateFillStartPoints: Boolean, seed: Long? = null) : MazeGenerator {

    private val rng = Random(seed ?: System.currentTimeMillis())

    /**
     * Generates a random rectangular maze. This is done by the following steps:
     *
     * <ul>
     *   <li>clear all cells, mark all except a border of 2 cells as PATH</li>
     *   <li>place some random possible positions</li>
     *   <li>while POSSIBLE positions are available, select a random one,
     * 			place a WALL cell there, and check if the neighbours of the cell may be new possibilities</li>
     *   <li>make outer walls</li>
     * </ul>
     */
    override fun generateMaze(width: Int, height: Int): Maze {
        // Prepare a rectangular map with the given dimensions
        val maze = Maze(width, height)

        // outside in ... because "why not?"
        maze.fill(0, 0, width, height, Maze.OUTSIDE)

        // fill with PATH, leave 2 fields border on each side
        maze.fill(1, 1, width - 2, height - 2, Maze.PATH)

        // place outer walls
        maze.frame(0, 0, width, height, Maze.WALL)

        generateMaze(maze)

        return maze
    }

    override fun generateMaze(maze: Maze): Maze {
        val width = maze.width
        val height = maze.height

        // set initial possible wall positions
        val templateStartPositions: List<Point> = buildList {
            for (y in 0..<height) {
                for (x in 0..<width) {
                    val v = maze[x, y]
                    if (v == Maze.POSSIBLE) {
                        add(Point(x, y))
                    }
                }
            }
        }

        val potentialWalls: RandomBuffer<Point> = RandomBuffer()

        if (templateStartPositions.isNotEmpty()) {
            // If the template contains start positions, we take those.
            potentialWalls.addAll(templateStartPositions)
        }

        if (potentialWalls.empty || templateFillStartPoints) {
            // if we have no potential walls or want to add more of them randomly, we add random potential wall positions
            // analyze the maze for positions that can become starting positions (all path fields that are surrounded by other path fields)
            val possibleStartPositions = RandomBuffer<Point>(random = rng)
            for (y in 0..<height) {
                for (x in 0..<width) {
                    val v = maze[x, y]
                    if (v == Maze.PATH && maze.checkAllNeighbors(x, y, Maze.PATH)) {
                        possibleStartPositions.add(Point(x, y))
                    }
                }
            }

            // approx. 5% of all cells become starting positions
            // we have to substract the already defined ones we might got from a template file
            val initialPositionCount = (possibleStartPositions.size / 20) - potentialWalls.size
            for (i in 0..<initialPositionCount) {
                val nextPossible = possibleStartPositions.next()
                maze[nextPossible.x, nextPossible.y] = Maze.POSSIBLE
                potentialWalls.add(nextPossible)
            }
        }

        // repeat until no more open possibilities
        while (!potentialWalls.empty) {
            // extract a random possible position
            val p = potentialWalls.next()

            // set as WALL
            val x = p.x
            val y = p.y
            if (maze[x, y] != Maze.POSSIBLE) {
                // if this position lost its "possible" status in the meantime, we just ignore it
                continue
            }
            maze[x, y] = Maze.WALL

            // invalidate surrounding possible positions
            for (iy in y - 1..y + 1) {
                for (ix in x - 1..x + 1) {
                    if (maze.isValid(ix, iy) && maze[ix, iy] == Maze.POSSIBLE) {
                        maze[ix, iy] = Maze.PATH
                    }
                }
            }

            // check new possibilities
            for (iy in y - 1..y + 1) {
                for (ix in x - 1..x + 1) {
                    if (maze.isValid(ix, iy) && maze[ix, iy] == Maze.WALL) {
                        maze.checkPositionAndMakeItAPotentialWall(potentialWalls, ix, iy, 0, 1)
                        maze.checkPositionAndMakeItAPotentialWall(potentialWalls, ix, iy, 0, -1)
                        maze.checkPositionAndMakeItAPotentialWall(potentialWalls, ix, iy, 1, 0)
                        maze.checkPositionAndMakeItAPotentialWall(potentialWalls, ix, iy, -1, 0)
                    }
                }
            }
        }
        return maze
    }

    private fun Maze.checkAllNeighbors(x: Int, y: Int, v: Byte): Boolean {
        for (iy in y - 1..y + 1) {
            for (ix in x - 1..x + 1) {
                if (!isValid(ix, iy) || this[ix, iy] != v) {
                    return false
                }
            }
        }
        return true
    }

    private fun Maze.isValid(x: Int, y: Int): Boolean {
        return (x > 0)
                && (x < width - 1)
                && (y > 0)
                && (y < height - 1)
                && (this[x, y] != Maze.OUTSIDE)
    }

    /**
     * Checks if the neighbour (x+dx,y+dy) of the cell (x,y) may be marked with POSSIBLE.
     * (dx,dy) specify a direction for a step to place POSSIBLE cells.
     *
     *
     * Either dx or dy MUST be 0, the other may be 1 or -1.
     * If the position (x+dx,y+dy) is viable, this cell is marked POSSIBLE.
     *
     *
     * A new cell can be marked POSSIBLE if
     *
     *  * (x,y) is valid,
     *  * (x+dx,y+dy) is valid (neighbour in direction),
     *  * (x+2dx,y+2dx) is valid (neighbour of neighbour in direction),
     *  * (x+dx,y+dy) is not already marked POSSIBLE
     *  * the left and right neighbours (w.r.t. the direction) of the new cell are not WALL
     *  * and the left and right neighbours of the neighbour of the new cell are not WALL
     *
     * @param potentialWalls the possible positions so far
     * @param x  X-coordinate
     * @param y  Y-coordinate
     * @param dx delta for x (-1, 0 or 1), if not 0, dy MUST be 0
     * @param dy delta for y (-1, 0 or 1), if not 0, dx MUST be 0
     */
    private fun Maze.checkPositionAndMakeItAPotentialWall(potentialWalls: RandomBuffer<Point>, x: Int, y: Int, dx: Int, dy: Int) {
        // check if cell, neighbor cell and neighbor's neighbour cell (towards that direction) are all valid (in bounds and not OUTSIDE)
        if (!isValid(x, y) || !isValid(x + dx, y + dy) || !isValid(x + 2 * dx, y + 2 * dy)) {
            return
        }

        // check neighbor
        var tmpx = x + dx
        var tmpy = y + dy

        if (this[tmpx, tmpy] == Maze.POSSIBLE) {
            return
        }
        if (!(this[tmpx, tmpy] != Maze.WALL &&
                    this[tmpx - dy, tmpy - dx] != Maze.WALL &&
                    this[tmpx + dy, tmpy + dx] != Maze.WALL)
        ) {
            return
        }

        // ok - now check neighbor's neighbor
        tmpx += dx
        tmpy += dy
        if (!(this[tmpx, tmpy] != Maze.WALL &&
                    this[tmpx - dy, tmpy - dx] != Maze.WALL &&
                    this[tmpx + dy, tmpy + dx] != Maze.WALL)
        ) {
            return
        }

        // ok - mark the designated position as possible
        // and increase possiblity count
        this[x + dx, y + dy] = Maze.POSSIBLE
        potentialWalls.add(Point(x + dx, y + dy))
    }

}