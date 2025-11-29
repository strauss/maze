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

package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.BaitEventListener
import de.dreamcube.mazegame.common.maze.BaitType

/**
 * [Trapeater] strategy. Uses A* for pathfinding. Target selection happens through manhattan distance. Players and other baits are completely ignored.
 * The bot sticks to a target until it is collected or the bot is teleported. This intentionally bad strategy is good enough for a [Trapeater]. It is
 * not easily adaptable to "real" strategies. Developing a better approach will be faster ... you have been warned!
 */
@Bot("trapeater", flavor = "I eat traps for breakfast!")
@Suppress("unused")
class Trapeater : SingleTargetAStar(), BaitEventListener {

    /**
     * The bait object for all traps currently in the maze.
     */
    private val traps: MutableSet<Bait> = HashSet()

    override fun getNextMove(): Move {
        if (currentTarget == null || currentTarget !in traps || path.isEmpty()) {
            selectTarget()
            path.clear()
        }
        return super.getNextMove()
    }

    private fun selectTarget() {
        val mySnapshot: PlayerSnapshot = mazeClient.ownPlayerSnapshot
        var currentMinDistance: Int = Integer.MAX_VALUE
        for (bait in traps) {
            val manhattanDistance: Int = getManhattanDistance(mySnapshot.x, bait.x, mySnapshot.y, bait.y)
            if (currentTarget == null) {
                currentTarget = bait
                currentMinDistance = manhattanDistance
                continue
            }
            if (manhattanDistance < currentMinDistance) {
                currentTarget = bait
                currentMinDistance = manhattanDistance
            }
        }
    }

    override fun onBaitAppeared(bait: Bait) {
        if (bait.type == BaitType.TRAP) {
            traps.add(bait)
        }
    }

    override fun onBaitVanished(bait: Bait) {
        traps.remove(bait)
        if (currentTarget == bait) {
            currentTarget = null
            path.clear()
        }
    }
}