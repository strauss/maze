package de.dreamcube.mazegame.common.util

import java.awt.RenderingHints

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
        RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY
    )
}