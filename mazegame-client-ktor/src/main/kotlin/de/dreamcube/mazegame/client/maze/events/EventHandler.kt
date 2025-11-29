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

package de.dreamcube.mazegame.client.maze.events

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType
import java.util.*

/**
 * Central class for handling all maze game client related events.
 */
class EventHandler {

    // All the lists separated by listener types
    private val mazeEventListeners: MutableList<MazeEventListener> = LinkedList()
    private val baitEventListeners: MutableList<BaitEventListener> = LinkedList()
    private val playerConnectionListeners: MutableList<PlayerConnectionListener> = LinkedList()
    private val playerMovementListeners: MutableList<PlayerMovementListener> = LinkedList()
    private val scoreChangeListeners: MutableList<ScoreChangeListener> = LinkedList()
    private val chatInfoListeners: MutableList<ChatInfoListener> = LinkedList()
    private val errorInfoListeners: MutableList<ErrorInfoListener> = LinkedList()
    private val clientConnectionStatusListeners: MutableList<ClientConnectionStatusListener> = LinkedList()
    private val speedChangedListeners: MutableList<SpeedChangedListener> = LinkedList()

    /**
     * Adds the given [EventListener] to all matching lists according to the implemented interfaces. This operation
     * costs O(1).
     *
     * The [de.dreamcube.mazegame.client.maze.strategy.Strategy] and the
     * [de.dreamcube.mazegame.client.maze.strategy.VisualizationComponent] are added automagically at the appropriate
     * time. All other listeners have to be added manually.
     */
    fun addEventListener(listener: EventListener) {
        // Feel the power of smart casts :-)
        if (listener is MazeEventListener) {
            mazeEventListeners.add(listener)
        }
        if (listener is BaitEventListener) {
            baitEventListeners.add(listener)
        }
        if (listener is PlayerConnectionListener) {
            playerConnectionListeners.add(listener)
        }
        if (listener is PlayerMovementListener) {
            playerMovementListeners.add(listener)
        }
        if (listener is ScoreChangeListener) {
            scoreChangeListeners.add(listener)
        }
        if (listener is ChatInfoListener) {
            chatInfoListeners.add(listener)
        }
        if (listener is ErrorInfoListener) {
            errorInfoListeners.add(listener)
        }
        if (listener is ClientConnectionStatusListener) {
            clientConnectionStatusListeners.add(listener)
        }
        if (listener is SpeedChangedListener) {
            speedChangedListeners.add(listener)
        }
    }

    /**
     * Removes the given [EventListener] from all matching lists according to the implemented interfaces. This operation
     * costs O(n).
     */
    fun removeEventListener(listener: EventListener) {
        // Feel the power of smart casts :-)
        if (listener is MazeEventListener) {
            mazeEventListeners.remove(listener)
        }
        if (listener is BaitEventListener) {
            baitEventListeners.remove(listener)
        }
        if (listener is PlayerConnectionListener) {
            playerConnectionListeners.remove(listener)
        }
        if (listener is PlayerMovementListener) {
            playerMovementListeners.remove(listener)
        }
        if (listener is ScoreChangeListener) {
            scoreChangeListeners.remove(listener)
        }
        if (listener is ChatInfoListener) {
            chatInfoListeners.remove(listener)
        }
        if (listener is ErrorInfoListener) {
            errorInfoListeners.remove(listener)
        }
        if (listener is ClientConnectionStatusListener) {
            clientConnectionStatusListeners.remove(listener)
        }
        if (listener is SpeedChangedListener) {
            speedChangedListeners.remove(listener)
        }
    }

    // All the events
    fun fireMazeReceived(width: Int, height: Int, mazeLines: List<String>) {
        for (listener in mazeEventListeners) {
            listener.onMazeReceived(width, height, mazeLines)
        }
    }

    fun fireBaitAppeared(bait: Bait) {
        for (listener in baitEventListeners) {
            listener.onBaitAppeared(bait)
        }
    }

    fun fireBaitVanished(bait: Bait) {
        for (listener in baitEventListeners) {
            listener.onBaitVanished(bait)
        }
    }

    fun firePlayerLogin(playerSnapshot: PlayerSnapshot) {
        for (listener in playerConnectionListeners) {
            listener.onPlayerLogin(playerSnapshot)
        }
    }

    fun fireOwnPlayerLogin(playerSnapshot: PlayerSnapshot) {
        for (listener in playerConnectionListeners) {
            listener.onOwnPlayerLogin(playerSnapshot)
        }
    }

    fun firePlayerLogout(playerSnapshot: PlayerSnapshot) {
        for (listener in playerConnectionListeners) {
            listener.onPlayerLogout(playerSnapshot)
        }
    }

    fun firePlayerAppear(playerSnapshot: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerAppear(playerSnapshot)
        }
    }

    fun firePlayerVanish(playerSnapshot: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerVanish(playerSnapshot)
        }
    }

    fun firePlayerStep(oldPosition: PlayerPosition, newPlayerSnapshot: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerStep(oldPosition, newPlayerSnapshot)
        }
    }

    fun firePlayerTurn(oldPosition: PlayerPosition, newPlayerSnapshot: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerTurn(oldPosition, newPlayerSnapshot)
        }
    }

    fun firePlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        for (listener in playerMovementListeners) {
            listener.onPlayerTeleport(oldPosition, newPlayerSnapshot, teleportType, causingPlayerId)
        }
    }

    fun fireScoreChange(oldScore: Int, newPlayerSnapshot: PlayerSnapshot) {
        for (listener in scoreChangeListeners) {
            listener.onScoreChange(oldScore, newPlayerSnapshot)
        }
    }

    fun fireClientInfo(message: String) {
        for (listener in chatInfoListeners) {
            listener.onClientInfo(message)
        }
    }

    fun fireServerInfo(message: String) {
        for (listener in chatInfoListeners) {
            listener.onServerInfo(message)
        }
    }

    fun firePlayerChat(playerId: Int, playerNick: String, message: String, whisper: Boolean) {
        for (listener in chatInfoListeners) {
            listener.onPlayerChat(playerId, playerNick, message, whisper)
        }
    }

    fun fireServerError(infoCode: InfoCode) {
        for (listener in errorInfoListeners) {
            listener.onServerError(infoCode)
        }
    }

    fun fireConnectionStatusChange(oldStatus: ConnectionStatus, newStatus: ConnectionStatus) {
        for (listener in clientConnectionStatusListeners) {
            listener.onConnectionStatusChange(oldStatus, newStatus)
        }
    }

    fun fireSpeedChanged(oldSpeed: Int, newSpeed: Int) {
        for (listener in speedChangedListeners) {
            listener.onSpeedChanged(oldSpeed, newSpeed)
        }
    }
}