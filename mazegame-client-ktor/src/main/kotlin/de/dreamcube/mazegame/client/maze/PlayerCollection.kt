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

import de.dreamcube.hornet_queen.set.PrimitiveIntSetB
import de.dreamcube.mazegame.common.maze.Player
import de.dreamcube.mazegame.common.maze.PlayerPositionChangeReason
import de.dreamcube.mazegame.common.maze.ViewDirection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A collection of [Player]s.
 */
class PlayerCollection {

    companion object {
        private val MOVE_REASONS: Set<PlayerPositionChangeReason> =
            setOf(PlayerPositionChangeReason.MOVE, PlayerPositionChangeReason.TURN)
    }

    /**
     * [Mutex] for thread- ... coroutine-safety
     */
    private val playerMutex = Mutex()

    /**
     * The internal structure holding all the [Player]s. The [MutableMap] is used for performance reasons.
     */
    private val playerIdToPlayerMap: MutableMap<Int, Player> = LinkedHashMap()

    /**
     * Internal set of player IDs that never received a score, ever. This is used to decide when to set the score offset.
     */
    private val virginScorePlayerIds: MutableSet<Int> = PrimitiveIntSetB()

    /**
     * Adds a new [Player] to this [PlayerCollection]. It should only be called whenever a player appears.
     */
    internal suspend fun addPlayer(player: Player): Boolean {
        playerMutex.withLock {
            if (!playerIdToPlayerMap.containsKey(player.id)) {
                playerIdToPlayerMap[player.id] = player
                virginScorePlayerIds.add(player.id)
                return true
            }
            return false
        }
    }

    /**
     * Removes the [Player] with the given [playerId]. It should only be called whenever a player vanishes.
     */
    internal suspend fun removePlayerById(playerId: Int): PlayerSnapshot? {
        playerMutex.withLock {
            val removedPlayer: Player? = playerIdToPlayerMap.remove(playerId)
            virginScorePlayerIds.remove(playerId) // just in case
            return removedPlayer?.view()?.takeSnapshot()
        }
    }

    /**
     * Retrieves a read-only [PlayerView] of the [Player] with the id [playerId]. If no such [Player] exists, null is returned. The view will directly
     * reflect all changes made to the player object and access will not be thread-safe.
     */
    private fun getPlayerViewById(playerId: Int): PlayerView? {
        return playerIdToPlayerMap[playerId]?.view()
    }

    /**
     * Retrieves a [PlayerSnapshot] of the current [Player] state.
     */
    internal suspend fun getPlayerSnapshot(playerId: Int): PlayerSnapshot? {
        playerMutex.withLock {
            return getPlayerViewById(playerId)?.takeSnapshot()
        }
    }

    /**
     * Changes the [Player]'s position according to the given coordinates ([newX] and [newY]) and the [newViewDirection]. A [Pair] consisting of the
     * [PlayerSnapshot] before the change and the [PlayerSnapshot] after the change is returned. If the [Player] with the given [playerId] does not
     * exist, null is returned. If the reason is either a STEP or TURN, the [Player]'s move counter is increased for calculating the ms/move.
     */
    internal suspend fun changePlayerPosition(
        playerId: Int,
        newX: Int,
        newY: Int,
        newViewDirection: ViewDirection,
        reason: PlayerPositionChangeReason
    ): Pair<PlayerSnapshot, PlayerSnapshot>? {
        playerMutex.withLock {
            val player: Player = playerIdToPlayerMap[playerId] ?: return null
            val playerView = player.view()
            val oldSnapshot = playerView.takeSnapshot()
            player.x = newX
            player.y = newY
            player.viewDirection = newViewDirection
            if (reason in MOVE_REASONS) {
                player.incrementMoveCounter()
            }
            val newSnapshot = playerView.takeSnapshot()
            return Pair(oldSnapshot, newSnapshot)
        }
    }

    /**
     * Changes the [Player]'s score to the given [newScore]. Returns the old score. Returns null, if no player with [playerId] exists in the
     * collection.
     */
    internal suspend fun changePlayerScore(playerId: Int, newScore: Int): Pair<Int, PlayerSnapshot>? {
        playerMutex.withLock {
            val player: Player = playerIdToPlayerMap[playerId] ?: return null
            val oldScore = player.score
            if (newScore == 0 && player.pointsPerMinute > 0) {
                // If the server resets the score, it is set to 0, but the ppm are higher than 0. This prevents trapeaters from being reset. Their ppm is
                // usually negative. There might still be a tiny little chance, but it is assumed to be an irrelevant border case.
                player.resetScore()
            } else {
                player.score = newScore
            }
            if (virginScorePlayerIds.contains(playerId)) {
                player.scoreOffset = newScore
                virginScorePlayerIds.remove(playerId)
            }
            val newSnapshot = player.view().takeSnapshot()
            return Pair(oldScore, newSnapshot)
        }
    }

    /**
     * Returns an immutable [List] of [PlayerSnapshot]s of all players that are currently playing. If the map changes or the player change, those
     * changes won't be reflected. Each [PlayerSnapshot] contains a read-only view of the actual [Player] object reflecting at least the player
     * changes.
     */
    internal suspend fun elements(): List<PlayerSnapshot> {
        playerMutex.withLock {
            return playerIdToPlayerMap.values.asSequence()
                .map { it.view().takeSnapshot() }
                .toList()
        }
    }
}