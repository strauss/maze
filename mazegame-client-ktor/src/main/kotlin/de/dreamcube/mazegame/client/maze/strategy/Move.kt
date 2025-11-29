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

import de.dreamcube.mazegame.common.maze.ViewDirection

enum class Move {
    /**
     * Don't move at all.
     */
    DO_NOTHING,

    /**
     * Turn left.
     */
    TURN_L,

    /**
     * Turn right.
     */
    TURN_R,

    /**
     * Step forward.
     */
    STEP;

    /**
     * Determines the new [ViewDirection], if this [Move] is applied, while being in [viewDirectionBefore].
     */
    fun getViewDirectionAfter(viewDirectionBefore: ViewDirection) {
        when (this) {
            DO_NOTHING, STEP -> viewDirectionBefore
            TURN_R -> viewDirectionBefore.turnRight()
            TURN_L -> viewDirectionBefore.turnLeft()
        }
    }
}