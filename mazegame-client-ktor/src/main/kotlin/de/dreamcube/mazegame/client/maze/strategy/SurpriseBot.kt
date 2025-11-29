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

/**
 * Interface for a special server-sided bot. Don't bother implementing it, it is not meant for you ... except you want
 * to implement your own "frenzy bot".
 */
interface SurpriseBot {
    /**
     * If true, this bot is in "surprise mode". If in this mode the server knows what to do :-)
     */
    fun surpriseModeActive(): Boolean
}