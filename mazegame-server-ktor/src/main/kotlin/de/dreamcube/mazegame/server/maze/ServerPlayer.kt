package de.dreamcube.mazegame.server.maze

import kotlin.math.round
import kotlin.random.Random

class ServerPlayer(
    val id: Int,
    val nick: String,
    var x: Int = -1,
    var y: Int = -1,
    var viewDirection: ViewDirection = ViewDirection.random(),
    var score: Int = 0,
    val loginTime: Long = System.currentTimeMillis(),
    var playStartTime: Long = System.currentTimeMillis(),
) {
    fun resetScore() {
        score = 0
        playStartTime = System.currentTimeMillis()
    }

    val totalPlayTime: Long
        get() = System.currentTimeMillis() - loginTime

    val currentPlayTime: Long
        get() = System.currentTimeMillis() - playStartTime

    val pointsPerMinute: Int
        get() {
            val minutes: Double = currentPlayTime.toDouble() / (60_000.0)
            return (round((score.toDouble() / minutes) * 100.0) / 100.0).toInt()
        }
}

enum class ViewDirection(val shortName: String) {
    NORTH("n"),
    EAST("e"),
    SOUTH("s"),
    WEST("w");

    companion object {
        private val rng = Random.Default

        fun random(): ViewDirection {
            val roll = rng.nextInt(4)
            return when (roll) {
                0 -> NORTH
                1 -> EAST
                2 -> SOUTH
                else -> WEST
            }
        }
    }

    fun turnRight(): ViewDirection =
        when (this) {
            NORTH -> EAST
            EAST -> SOUTH
            SOUTH -> WEST
            WEST -> NORTH
        }

    fun turnLeft(): ViewDirection =
        when (this) {
            NORTH -> WEST
            EAST -> NORTH
            SOUTH -> EAST
            WEST -> SOUTH
        }
}

enum class PlayerPositionChange(val shortName: String) {
    TELEPORT("tel"),
    APPEAR("app"),
    VANISH("van"),
    MOVE("mov"),
    TURN("trn")
}

enum class TeleportType(val shortName: String) {
    TRAP("t"),
    COLLISION("c")
}
