package de.dreamcube.mazegame.common.maze

import kotlin.math.round
import kotlin.random.Random

/**
 * Represents a player on either the server or client side. The server assigns an [id], which is transmitted to the client. The client assigns a
 * [nick] and the server accepts it, if it is still available. The current coordinate is stored in the [x] and [y] values. The class also contains the
 * [viewDirection] and the [score]. For statistics, also the [loginTime] and the [playStartTime] is kept.
 */
class Player(
    val id: Int,
    val nick: String,
    var x: Int = -1,
    var y: Int = -1,
    var viewDirection: ViewDirection = ViewDirection.random(),
    score: Int = 0,
    val loginTime: Long = System.currentTimeMillis(),
    var playStartTime: Long = System.currentTimeMillis(),
) {
    var score: Int = score
        set(value) {
            // If the server resets the score, it is set to 0, but the ppm are higher than 0. This prevents trapeaters from being reset. Their ppm is
            // usually negative. There might still be a tiny little chance, but it is assumed to be an irrelevant border case.
            if (value == 0 && pointsPerMinute > 0) {
                resetScore()
            } else {
                field = value
            }
        }

    /**
     * Resets the score. It is mainly used on the server side, but can also be used on the client side, if the score is set to 0.
     */
    fun resetScore() {
        score = 0
        playStartTime = System.currentTimeMillis()
    }

    /**
     * Time since login in milliseconds.
     */
    val totalPlayTime: Long
        get() = System.currentTimeMillis() - loginTime

    /**
     * Time since last score reset in milliseconds.
     */
    val currentPlayTime: Long
        get() = System.currentTimeMillis() - playStartTime

    /**
     * Points per minute since last score reset.
     */
    val pointsPerMinute: Double
        get() {
            val minutes: Double = currentPlayTime.toDouble() / (60_000.0)
            return round((score.toDouble() / minutes) * 100.0) / 100.0
        }
}

/**
 * The view direction as orientation.
 */
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

        @JvmStatic
        fun fromShortName(shortName: String): ViewDirection = when (shortName) {
            NORTH.shortName -> NORTH
            EAST.shortName -> EAST
            SOUTH.shortName -> SOUTH
            WEST.shortName -> WEST
            else -> TODO("ERROR")
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
    TURN("trn");

    companion object {
        fun fromShortName(shortName: String): PlayerPositionChange = when (shortName) {
            MOVE.shortName -> MOVE
            TURN.shortName -> TURN
            TELEPORT.shortName -> TELEPORT
            APPEAR.shortName -> APPEAR
            VANISH.shortName -> VANISH
            else -> TODO("ERROR")
        }
    }
}

enum class TeleportType(val shortName: String) {
    TRAP("t"),
    COLLISION("c");

    companion object {
        fun fromShortName(shortName: String): TeleportType = when (shortName) {
            TRAP.shortName -> TRAP
            COLLISION.shortName -> COLLISION
            else -> TODO("ERROR")
        }
    }
}
