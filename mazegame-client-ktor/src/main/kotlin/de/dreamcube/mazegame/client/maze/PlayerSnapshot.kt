package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.ViewDirection

/**
 * Contains a snapshot of the internal player state at the moment the object is created. Useful for strategies, but not for live surveillance.
 */
class PlayerSnapshot(val view: PlayerView) {
    val id: Int = view.id
    val nick: String = view.nick
    val x: Int = view.x
    val y: Int = view.y
    val viewDirection: ViewDirection = view.viewDirection
    val score: Int = view.score
    val totalPlayTime: Long = view.totalPlayTime
    val currentPlayTime: Long = view.currentPlayTime
    val pointsPerMinute: Double = view.pointsPerMinute
}