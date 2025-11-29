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

package de.dreamcube.mazegame.common.api

import de.dreamcube.mazegame.common.maze.BaitType

/**
 * The server control command for teleportation.
 */
data class TeleportCommandDto(val id: Int, val x: Int, val y: Int)

/**
 * The server control command for putting a bait.
 */
data class PutBaitCommandDto(
    val baitType: BaitType,
    val x: Int,
    val y: Int,
    val visible: Boolean = true,
    val reappearOffset: Long = 0L
)
