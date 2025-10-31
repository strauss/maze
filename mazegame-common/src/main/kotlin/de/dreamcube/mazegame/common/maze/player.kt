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
    var score: Int = 0,
    val loginTime: Long = System.currentTimeMillis(),
    var playStartTime: Long = System.currentTimeMillis(),
    moveCounter: Int = 0,
    var scoreOffset: Int = score
) {

    var moveCounter: Int = moveCounter
        private set

    /**
     * Resets the score. It is mainly used on the server side, but can also be used on the client side, if the score is set to 0.
     */
    fun resetScore() {
        score = 0
        internalResetScoreRelatedInformation()
    }

    private fun internalResetScoreRelatedInformation() {
        scoreOffset = 0
        moveCounter = 0
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
            return round(((score - scoreOffset).toDouble() / minutes) * 100.0) / 100.0
        }

    /**
     * Average time per move for the current play time.
     */
    val moveTime: Double
        get() = if (moveCounter > 0) round(currentPlayTime.toDouble() * 100.0 / moveCounter.toDouble()) / 100.0 else Double.NaN

    /**
     * Increases the internal move counter by 1.
     */
    fun incrementMoveCounter() {
        moveCounter += 1
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
            else -> throw IllegalArgumentException("Incorrect view direction: $shortName")
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

enum class PlayerPositionChangeReason(val shortName: String) {
    TELEPORT("tel"),
    APPEAR("app"),
    VANISH("van"),
    MOVE("mov"),
    TURN("trn");

    companion object {
        fun fromShortName(shortName: String): PlayerPositionChangeReason = when (shortName) {
            MOVE.shortName -> MOVE
            TURN.shortName -> TURN
            TELEPORT.shortName -> TELEPORT
            APPEAR.shortName -> APPEAR
            VANISH.shortName -> VANISH
            else -> throw IllegalArgumentException("Incorrect player position change reason: $shortName")
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
            else -> throw IllegalArgumentException("Incorrect teleport type: $shortName")
        }
    }
}

/**
 * Simple data class representing the position of a player and its viewDirection but without everything else.
 */
data class PlayerPosition(val x: Int, val y: Int, val viewDirection: ViewDirection) {
    fun whenRight(): PlayerPosition = PlayerPosition(x, y, viewDirection.turnRight())
    fun whenLeft(): PlayerPosition = PlayerPosition(x, y, viewDirection.turnLeft())
    fun whenStep(): PlayerPosition {
        val newX = when (viewDirection) {
            ViewDirection.NORTH, ViewDirection.SOUTH -> x
            ViewDirection.EAST -> x + 1
            ViewDirection.WEST -> x - 1
        }
        val newY = when (viewDirection) {
            ViewDirection.NORTH -> y - 1
            ViewDirection.EAST, ViewDirection.WEST -> y
            ViewDirection.SOUTH -> y + 1
        }
        return PlayerPosition(newX, newY, viewDirection)
    }
}
