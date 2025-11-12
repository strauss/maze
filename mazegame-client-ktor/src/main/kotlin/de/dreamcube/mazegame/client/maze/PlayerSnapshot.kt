package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.ViewDirection

/**
 * Contains a snapshot of the internal player state at the moment the object is created. Useful for strategies, but not
 * for live surveillance. If you really want to do it, you can use the [view] reference for a read-only realtime view
 * on the player.
 */
class PlayerSnapshot(val view: PlayerView) {
    val id: Int
        get() = view.id
    val nick: String
        get() = view.nick
    val flavor: String?
        get() = view.flavor
    val x: Int = view.x
    val y: Int = view.y
    val viewDirection: ViewDirection = view.viewDirection
    val score: Int = view.score
    val totalPlayTime: Long = view.totalPlayTime
    val currentPlayTime: Long = view.currentPlayTime
    val pointsPerMinute: Double = view.pointsPerMinute
    val moveTime: Double = view.moveTime
    val scoreOffset: Int = view.scoreOffset

    /**
     * For convenience: Creates a [PlayerPosition] object, each time it is accessed.
     */
    val position: PlayerPosition
        get() = PlayerPosition(x, y, viewDirection)

    /**
     * Returns the score without the [scoreOffset].
     */
    val localScore: Int
        get() = score - scoreOffset
}