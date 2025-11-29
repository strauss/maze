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

package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.BaitEventListener
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.MazeEventListener
import de.dreamcube.mazegame.client.maze.events.PlayerMovementListener
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The maze representation required for rendering the maze. It is updated using client events. Whenever the content of a
 * path field changes, the [UiController] is told to update the [MazePanel] at the corresponding field.
 */
class MazeModel() : MazeEventListener, BaitEventListener, PlayerMovementListener, ClientConnectionStatusListener {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MazeModel::class.java)
    }

    /**
     * Occupation status of a path.
     */
    enum class PathOccupationStatus {
        /**
         * The path is empty.
         */
        EMPTY,

        /**
         * The path contains a player.
         */
        PLAYER,

        /**
         * The path contains a bait.
         */
        BAIT,

        /**
         * The path contains a player and a bait simultaneously (forbidden).
         */
        CROWDED
    }

    /**
     * Interface defining a [MazeField], namely a single cell in the maze. Most of them are represented by singleton
     * objects. Only the paths have actual objects that can change.
     */
    sealed interface MazeField

    /**
     * An "unknown" maze field (the server would send a '?')
     */
    object UnknownMazeField : MazeField

    /**
     * An "outside" maze field (the server would send a '-')
     */
    object OutsideMazeField : MazeField

    /**
     * A "wall" maze field (the server would send a '#')
     */
    object WallMazeField : MazeField

    /**
     * A path field may contain a bait or a player. The bait is represented by its [BaitType]. The player is represented
     * by its current [PlayerSnapshot].
     */
    class PathMazeField() : MazeField {
        /**
         * Bait on this path field, null if there is none.
         */
        var bait: BaitType? = null
            internal set

        /**
         * Player on this path field, null if there is none.
         */
        var player: PlayerSnapshot? = null
            internal set

        /**
         * Derived occupation status.
         *
         * - [PathOccupationStatus.EMPTY] if neither player nor bait are set
         * - [PathOccupationStatus.BAIT] if the bait is set but the player is not
         * - [PathOccupationStatus.PLAYER] if the player is set but the bait is not
         * - [PathOccupationStatus.CROWDED] if both player and bait are set (forbidden)
         */
        val occupationStatus
            get() =
                when {
                    bait == null && player == null -> PathOccupationStatus.EMPTY
                    bait != null && player == null -> PathOccupationStatus.BAIT
                    bait == null && player != null -> PathOccupationStatus.PLAYER
                    else -> PathOccupationStatus.CROWDED // this should not happen, but you never know
                }
    }

    init {
        UiController.prepareEventListener(this)
    }

    /**
     * Width of the maze.
     */
    internal var width: Int = 0
        private set

    /**
     * Height of the maze.
     */
    internal var height: Int = 0
        private set

    /**
     * Internal 1D-Array representation of the maze.
     */
    var internalMaze: Array<MazeField> = Array(0) { UnknownMazeField }

    /**
     * Did we receive the maze?
     */
    var mazeReceived: Boolean = false
        private set

    /**
     * Function for resetting this part of the UI.
     */
    private fun reset() {
        width = 0
        height = 0
        internalMaze = Array(0) { UnknownMazeField }
        mazeReceived = false
    }

    /**
     * Parses the received maze into the UI maze structure.
     */
    override fun onMazeReceived(width: Int, height: Int, mazeLines: List<String>) {
        this.width = width
        this.height = height
        internalMaze = Array(width * height) { UnknownMazeField }
        var y = 0
        for (currentLine: String in mazeLines) {
            if (currentLine.length != width) {
                LOGGER.error("Maze line should have length of $width but has length ${currentLine.length}.")
            }
            var x = 0
            for (c: Char in currentLine) {
                this[x, y] = when (c) {
                    '-' -> OutsideMazeField
                    '#' -> WallMazeField
                    '.' -> PathMazeField()
                    else -> UnknownMazeField
                }
                x += 1
            }
            y += 1
        }
        if (y != height) {
            LOGGER.error("Received $y maze lines but expected $height. Some fields will be shown as 'UNKNOWN'.")
        }
        mazeReceived = true
    }

    private operator fun set(x: Int, y: Int, value: MazeField) {
        val position: Int = y * width + x
        internalMaze[position] = value
    }

    internal operator fun get(x: Int, y: Int): MazeField {
        val position: Int = y * width + x
        return internalMaze[position]
    }

    override fun onBaitAppeared(bait: Bait) {
        val mazeField = this[bait.x, bait.y]
        if (mazeField is PathMazeField) {
            mazeField.bait = bait.type
            UiController.triggerMazeUpdate(bait.x, bait.y)
        }
    }

    override fun onBaitVanished(bait: Bait) {
        val mazeField = this[bait.x, bait.y]
        if (mazeField is PathMazeField) {
            mazeField.bait = null
            UiController.triggerMazeUpdate(bait.x, bait.y)
        }
    }

    override fun onPlayerAppear(playerSnapshot: PlayerSnapshot) {
        updatePlayerAtField(playerSnapshot)
    }

    override fun onPlayerTurn(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        if (oldPosition.x == newPlayerSnapshot.x && oldPosition.y == newPlayerSnapshot.y) {
            updatePlayerAtField(newPlayerSnapshot)
        } else {
            LOGGER.warn("Turn command with position change.")
            movePlayer(oldPosition, newPlayerSnapshot)
        }
    }

    private fun updatePlayerAtField(playerSnapshot: PlayerSnapshot) {
        val mazeField = this[playerSnapshot.x, playerSnapshot.y]
        if (mazeField is PathMazeField) {
            mazeField.player = playerSnapshot
            if (mazeField.occupationStatus == PathOccupationStatus.PLAYER) {
                UiController.triggerMazeUpdate(playerSnapshot.x, playerSnapshot.y)
            }
        }
    }

    override fun onPlayerVanish(playerSnapshot: PlayerSnapshot) {
        val mazeField = this[playerSnapshot.x, playerSnapshot.y]
        if (mazeField is PathMazeField) {
            mazeField.player = null
            UiController.triggerMazeUpdate(playerSnapshot.x, playerSnapshot.y)
        }
    }

    override fun onPlayerStep(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        movePlayer(oldPosition, newPlayerSnapshot)
    }

    override fun onPlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        movePlayer(oldPosition, newPlayerSnapshot)
    }

    private fun movePlayer(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        val previousMazeField = this[oldPosition.x, oldPosition.y]
        if (previousMazeField is PathMazeField) {
            previousMazeField.player = null
            UiController.triggerMazeUpdate(oldPosition.x, oldPosition.y)
        }
        val mazeField = this[newPlayerSnapshot.x, newPlayerSnapshot.y]
        if (mazeField is PathMazeField) {
            mazeField.player = newPlayerSnapshot
            UiController.triggerMazeUpdate(newPlayerSnapshot.x, newPlayerSnapshot.y)

            // If we have a marked player, we write their maze coordinates to the status bar
            val playerToMark: UiPlayerInformation? = UiController.markerPane.playerToMark
            if (playerToMark != null && playerToMark.id == newPlayerSnapshot.id) {
                UiController.uiScope.launch {
                    UiController.updatePositionStatus(newPlayerSnapshot.x, newPlayerSnapshot.y)
                }
            }
        }
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        if (newStatus == ConnectionStatus.DEAD) {
            reset()
        }
    }
}