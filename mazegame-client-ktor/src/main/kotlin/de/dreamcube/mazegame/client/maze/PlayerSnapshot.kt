package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.ViewDirection

/**
 * Contains a snapshot of the internal player state at the moment the object is created. Useful for strategies, but not for live surveillance.
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
    val position: PlayerPosition
        get() = PlayerPosition(x, y, viewDirection)
    val localScore: Int
        get() = score - scoreOffset
}