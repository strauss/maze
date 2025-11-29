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

package de.dreamcube.mazegame.common.util

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.Rectangle2D
import kotlin.math.round

object VisualizationHelper {

    @JvmStatic
    internal fun createRenderingHints(vararg configuration: Pair<RenderingHints.Key, Any>): RenderingHints {
        val result = RenderingHints(emptyMap<RenderingHints.Key, Any>())
        for ((key, value) in configuration) {
            result.put(key, value)
        }
        return result
    }

    /**
     * Creates a set of default rendering hints suitable for a high quality visualization. It is also used by the player
     * marker.
     */
    @JvmStatic
    fun createDefaultRenderingHints() = createRenderingHints(
        RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,
        RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,
        RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_OFF
    )

    /**
     * Draws the given [text] centric inside the rectangle defined by the coordinates and the dimensions. It is used to
     * render the baits in the center of the maze cells. It can also be used to display text (or numbers) inside the
     * maze cells for visualizing the calculations of the strategy. Feel free to use it.
     */
    @JvmStatic
    fun drawTextCentric(
        g2: Graphics2D,
        text: String,
        x: Int,
        y: Int,
        recWith: Int,
        recHeight: Int
    ) {
        g2.setRenderingHints(createDefaultRenderingHints())
        val fontRenderContext: FontRenderContext = g2.fontRenderContext
        val layout = TextLayout(text, g2.font, fontRenderContext)
        val visualBounds: Rectangle2D = layout.bounds
        val cx = x + recWith / 2 + 1
        val cy = y + recHeight / 2 + 1
        val xx = round(cx - (visualBounds.x + visualBounds.width / 2.0)).toFloat()
        val yy = round(cy - (visualBounds.y + visualBounds.height / 2.0)).toFloat()

        layout.draw(g2, xx, yy)
    }
}