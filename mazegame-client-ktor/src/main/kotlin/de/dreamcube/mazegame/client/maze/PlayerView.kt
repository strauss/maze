package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.Player

/**
 * Read-only representation of the current state of a [Player]. Directly reflects all changes in a non-thread-safe manner.
 */
class PlayerView(private val player: Player) {
    val id: Int
        get() = player.id
    val nick: String
        get() = player.nick
    val x: Int
        get() = player.x
    val y: Int
        get() = player.y
    val viewDirection
        get() = player.viewDirection
    val score: Int
        get() = player.score
    val loginTime: Long
        get() = player.loginTime
    val playStartTime: Long
        get() = player.playStartTime
    val totalPlayTime: Long
        get() = player.totalPlayTime
    val currentPlayTime: Long
        get() = player.currentPlayTime
    val pointsPerMinute: Double
        get() = player.pointsPerMinute
    val moveTime: Double
        get() = player.moveTime
}