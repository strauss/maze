/*
 * Maze Game
 * Copyright (c) 2026 Sascha Strauß
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

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.PlayerMovementListener
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType
import de.dreamcube.mazegame.common.util.VisualizationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent

/**
 * Part of the glass pane that can be activated. It shows the player names as label above them.
 */
class LabelPane() : JComponent(), PlayerMovementListener {

    companion object {
        private val BASE_FONT_LABEL = Font("SansSerif", Font.BOLD, 0)
    }

    /**
     * Reference to the maze panel.
     */
    private val mazePanel: MazePanel
        get() = UiController.mazePanel

    /**
     * The current zoom.
     */
    private val zoom
        get() = mazePanel.zoom

    /**
     * The current offset (origin of the top-left pixel of the maze).
     */
    private val offset
        get() = mazePanel.offset

    private val qualityHints: RenderingHints = VisualizationHelper.createDefaultRenderingHints()

    private val currentPlayerSnapshots: MutableMap<Int, PlayerSnapshot> = HashMap()

    private val playerListMutex = Mutex()

    internal var colorDistributionMap = AtomicReference<Map<Int, Color>>(mapOf())

    override fun onPlayerAppear(playerSnapshot: PlayerSnapshot) {
        UiController.bgScope.launch {
            playerListMutex.withLock {
                currentPlayerSnapshots[playerSnapshot.id] = playerSnapshot
            }
        }
    }

    override fun onPlayerStep(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        UiController.bgScope.launch {
            playerListMutex.withLock {
                currentPlayerSnapshots[newPlayerSnapshot.id] = newPlayerSnapshot
            }
        }
    }

    override fun onPlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        UiController.bgScope.launch {
            playerListMutex.withLock {
                currentPlayerSnapshots[newPlayerSnapshot.id] = newPlayerSnapshot
            }
        }
    }

    override fun onPlayerVanish(playerSnapshot: PlayerSnapshot) {
        UiController.bgScope.launch {
            playerListMutex.withLock {
                currentPlayerSnapshots.remove(playerSnapshot.id)
            }
        }
    }

    override fun paintComponent(g: Graphics?) {
        val g2 = g as Graphics2D

        g2.run {
            setRenderingHints(qualityHints)
            val playerSnapshots: List<PlayerSnapshot> = runBlocking {
                playerListMutex.withLock {
                    currentPlayerSnapshots.values.toList()
                }
            }
            val colors = colorDistributionMap.get()
            val labelFont = BASE_FONT_LABEL.deriveFont(zoom.toFloat() * 0.65f)
            for (snapshot in playerSnapshots) {
                val id = snapshot.id
                val lX = offset.x + snapshot.x * zoom
                val lY = offset.y + (snapshot.y - 1) * zoom // the label will be painted one field above

                color = colors[id] ?: Color.darkGray
                font = labelFont
                VisualizationHelper.drawTextCentric(this, snapshot.nick, lX, lY, zoom, zoom)
            }
        }
    }

    internal suspend fun reset() {
        isVisible = false
        playerListMutex.withLock {
            currentPlayerSnapshots.clear()
        }
    }
}