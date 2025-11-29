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

package de.dreamcube.mazegame.client.maze.strategy.vizualisation

import de.dreamcube.mazegame.client.maze.strategy.VisualizationComponent
import de.dreamcube.mazegame.common.util.VisualizationHelper.createDefaultRenderingHints
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D

/**
 * Example visualization. It showcases how to get the color of the selected player and how to access some of the other
 * fields from [VisualizationComponent].
 */
class DebugVisualization : VisualizationComponent() {

    override val activateImmediately: Boolean
        get() = true

    private val qualityHints = createDefaultRenderingHints()

    override fun paintComponent(g: Graphics?) {
        val g2 = g as Graphics2D
        g2.run {
            setRenderingHints(qualityHints)
            color = selectedPlayerId?.let { getPlayerColor(it) } ?: Color.magenta
            font = g2.font.deriveFont(50.0f)
            drawString("Debug visualization", 0, 50)
            drawString("Offset: (${offset.x}/${offset.y})", 0, 100)
            drawString("Zoom: $zoom", 0, 150)
            drawRect(0, 0, width - 1, height - 1)
        }
    }
}