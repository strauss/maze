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

import de.dreamcube.mazegame.client.maze.events.ErrorInfoListener
import de.dreamcube.mazegame.client.maze.strategy.vizualisation.DebugVisualization
import de.dreamcube.mazegame.common.maze.InfoCode
import kotlin.random.Random

/**
 * A dummy bot implementation. The design is based on the original dummy bot. It ships with a debug visualization,
 * showcasing, how visualizations can be done in general. It can also be used as fallback strategy by incorporating
 * a reference of [Aimless] in your own strategy.
 */
@Bot("dummy", flavor = "I run against walls!")
@Suppress("unused")
class Aimless : Strategy(), ErrorInfoListener {

    private var nextMove: Move? = null
    private val rng = Random

    private val visualization = DebugVisualization()

    override fun getNextMove(): Move {
        botDelayInMs = rng.nextInt(mazeClient.gameSpeed)
        return nextDummyMove()
    }

    override fun onServerError(infoCode: InfoCode) {
        if (infoCode == InfoCode.WALL_CRASH) {
            val r = rng.nextDouble()
            nextMove = if (r < 0.5) Move.TURN_L else Move.TURN_R
        }
    }

    override fun getVisualizationComponent(): VisualizationComponent {
        return visualization
    }

    /**
     * Can be used to determine the next dummy move for other bots as fallback. If you use it, don't forget to register
     * your [Aimless] reference with the client's event handler. If you forget to do so, the wall crash detection will
     * not work.
     */
    fun nextDummyMove(): Move {
        if (nextMove != null) {
            val moveToReturn: Move = nextMove!!
            nextMove = null
            return moveToReturn
        }
        val r = rng.nextDouble()
        return when {
            r < 0.05 -> Move.TURN_L
            r < 0.1 -> Move.TURN_R
            else -> Move.STEP
        }
    }

}