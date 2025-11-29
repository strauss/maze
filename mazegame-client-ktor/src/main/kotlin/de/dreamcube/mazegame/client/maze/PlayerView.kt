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

package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.Player

/**
 * Read-only representation of the current state of a [Player]. Directly reflects all changes in a non-thread-safe manner.
 */
class PlayerView internal constructor(private val player: Player) {
    val id: Int
        get() = player.id
    val nick: String
        get() = player.nick
    val flavor: String?
        get() = player.flavor
    val x: Int
        get() = player.x
    val y: Int
        get() = player.y
    val viewDirection
        get() = player.viewDirection
    val score: Int
        get() = player.score
    val loginTime: Long
        get() = player.loginTime
    val playStartTime: Long
        get() = player.playStartTime
    val totalPlayTime: Long
        get() = player.totalPlayTime
    val currentPlayTime: Long
        get() = player.currentPlayTime
    val pointsPerMinute: Double
        get() = player.pointsPerMinute
    val moveTime: Double
        get() = player.moveTime
    val scoreOffset: Int
        get() = player.scoreOffset

    /**
     * Takes a snapshot of the current state. The caller is responsible for thread-safety of the snapshot creation.
     */
    fun takeSnapshot() = PlayerSnapshot(this)
}

/**
 * Creates a read-only view of the player object.
 */
fun Player.view(): PlayerView = PlayerView(this)