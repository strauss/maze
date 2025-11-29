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
import de.dreamcube.mazegame.client.maze.strategy.*
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType
import de.dreamcube.mazegame.common.maze.ViewDirection
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.KeyEvent

/**
 * Simplest imaginable spectator strategy. Just does nothing.
 */
@Suppress("unused")
@Bot("spectator", isSpectator = true, flavor = "Who will win? Nobody knows!")
class Spectator : Strategy() {

    /**
     * A spectator does ... nothing
     */
    override fun getNextMove(): Move = Move.DO_NOTHING

}

/**
 * Human strategy with keyboard control. Every command is represented by one of the arrow keys.
 */
@Suppress("unused")
@Bot("humanFirstPerson", isHuman = true, flavor = "User arrow keys: left/right for turn and up for step")
class HumanPlayerFirstPerson : Strategy(), ClientConnectionStatusListener {

    private val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    private val keyEventDispatcher = KeyEventDispatcher { e ->
        if (e != null && e.id == KeyEvent.KEY_PRESSED) {
            when (e.keyCode) {
                KeyEvent.VK_UP -> nextMove = Move.STEP
                KeyEvent.VK_LEFT -> nextMove = Move.TURN_L
                KeyEvent.VK_RIGHT -> nextMove = Move.TURN_R
            }
        }
        false
    }

    private var nextMove = Move.DO_NOTHING

    override fun getNextMove(): Move {
        val move = nextMove
        nextMove = Move.DO_NOTHING
        return move
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        when (newStatus) {
            ConnectionStatus.PLAYING -> {
                kfm.addKeyEventDispatcher(keyEventDispatcher)
            }

            ConnectionStatus.DEAD -> {
                kfm.removeKeyEventDispatcher(keyEventDispatcher)
            }

            else -> {
                // do nothing
            }
        }
    }
}

/**
 * Human strategy with keyboard control. Here you press in the direction you want to go and the bot does the turning
 * automatically.
 */
@Suppress("unused")
@Bot("humanThirdPerson", isHuman = true, flavor = "Use all four arrow keys")
class HumanPlayerThirdPerson : Strategy(), ClientConnectionStatusListener {

    private val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    private val keyEventDispatcher = KeyEventDispatcher { e ->
        if (e != null && e.id == KeyEvent.KEY_PRESSED) {
            val viewDirection = this.viewDirection
            when (e.keyCode) {
                KeyEvent.VK_UP -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.NORTH -> Move.STEP
                        ViewDirection.WEST -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }

                KeyEvent.VK_LEFT -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.WEST -> Move.STEP
                        ViewDirection.SOUTH -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }

                KeyEvent.VK_RIGHT -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.EAST -> Move.STEP
                        ViewDirection.NORTH -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }

                KeyEvent.VK_DOWN -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.SOUTH -> Move.STEP
                        ViewDirection.EAST -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }
            }
        }
        false
    }

    private var nextMove = Move.DO_NOTHING

    private val viewDirection: ViewDirection
        get() = mazeClient.ownPlayerSnapshot.viewDirection

    override fun getNextMove(): Move {
        val move = nextMove
        nextMove = Move.DO_NOTHING
        return move
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        when (newStatus) {
            ConnectionStatus.PLAYING -> {
                kfm.addKeyEventDispatcher(keyEventDispatcher)
            }

            ConnectionStatus.DEAD -> {
                kfm.removeKeyEventDispatcher(keyEventDispatcher)
            }

            else -> {
                // do nothing
            }
        }
    }
}

/**
 * Human strategy with mouse control. The player clicks on the bait and the "almost bot" will move automatically towards
 * the bait. Automatically marks the player and starts the visualization. Whenever a bait is selected, the path is also
 * shown in the maze.
 */
@Suppress("unused")
@Bot("clickAndCollect", isHuman = true, flavor = "Click on bait, I will collect it!")
class HumanPlayerLazy : SingleTargetAStar(), ClientConnectionStatusListener, MazeCellListener,
    BaitEventListener {

    private val visualization = object : VisualizationComponent() {

        private val thickStroke = BasicStroke(4.0f)
        private val normalStroke = BasicStroke(3f)

        override val activateImmediately: Boolean
            get() = true

        override fun paintComponent(g: Graphics?) {
            val target = currentTarget ?: return
            val g2 = g as Graphics2D
            g2.run {
                // mark target
                color = getPlayerColor(UiController.ownId)
                stroke = thickStroke
                val xx = offset.x + target.x * zoom
                val yy = offset.y + target.y * zoom
                drawRect(xx, yy, zoom, zoom)

                // draw path
                stroke = normalStroke
                var px = -1
                var py = -1
                val path = path.toList() // copy for avoiding exception ... probably won't work
                for (pos in path) {
                    val cx = offset.x + pos.x * zoom + zoom / 2
                    val cy = offset.y + pos.y * zoom + zoom / 2
                    if (px >= 0 && py >= 0) {
                        drawLine(px, py, cx, cy)
                    }
                    px = cx
                    py = cy
                }
            }
        }
    }

    override fun getVisualizationComponent(): VisualizationComponent? {
        return visualization
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        when (newStatus) {
            ConnectionStatus.PLAYING -> {
                UiController.addMazeCellListener(this)
                UiController.activateHoverMarks()
                UiController.uiScope.launch {
                    UiController.uiPlayerCollection.getIndex(UiController.ownId)?.let {
                        UiController.scoreTable.selectIndex(it)
                    }
                }
            }

            ConnectionStatus.DEAD -> {
                UiController.removeMazeCellListener(this)
                UiController.deactivateHoverMarks()
            }

            else -> {
                // do nothing
            }
        }
    }

    override fun onMazeCellSelected(x: Int, y: Int, mazeField: MazeModel.MazeField) {
        if (mazeField is MazeModel.PathMazeField && mazeField.occupationStatus == MazeModel.PathOccupationStatus.BAIT) {
            val bait: Bait? = runBlocking { mazeClient.getBaitAt(x, y) }
            bait?.let {
                currentTarget = it
                path.clear()
            }
        }
    }

    override fun onBaitAppeared(bait: Bait) {
        UiController.activateHoverMarks()
    }

    override fun onBaitVanished(bait: Bait) {
        if (currentTarget == bait) {
            currentTarget = null
            path.clear()
        }
    }

    override fun onPlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        super.onPlayerTeleport(oldPosition, newPlayerSnapshot, teleportType, causingPlayerId)
        UiController.activateHoverMarks()
    }

}