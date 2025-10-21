package de.dreamcube.mazegame.client

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.client.maze.events.PlayerMovementListener
import de.dreamcube.mazegame.client.maze.events.ScoreChangeListener
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

object HeadlessPlayerConnectionLogger : PlayerConnectionListener {

    val LOGGER: Logger = LoggerFactory.getLogger(HeadlessPlayerConnectionLogger::class.java)

    override fun onPlayerLogin(playerSnapshot: PlayerSnapshot) {
        val nick = playerSnapshot.nick
        val playerId = playerSnapshot.id
        LOGGER.info("Player '$nick ($playerId)' logged in.")
    }

    override fun onOwnPlayerLogin(playerSnapshot: PlayerSnapshot) {
        val nick = playerSnapshot.nick
        val playerId = playerSnapshot.id
        LOGGER.info("The own player '$nick ($playerId)' finally logged in.")
    }

    override fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        val nick = playerSnapshot.nick
        val playerId = playerSnapshot.id
        val totalPlayTime: String = playerSnapshot.totalPlayTime.milliseconds.toString()
        val currentPlayTime: String = playerSnapshot.currentPlayTime.milliseconds.toString()
        val ppm: String = playerSnapshot.pointsPerMinute.toString()
        val moveTime: String = playerSnapshot.moveTime.toString()
        LOGGER.info("Player '$nick ($playerId)' logged out. Total playtime was: $totalPlayTime. Current playtime was: $currentPlayTime. Current points per minute was $ppm. Current move time was: $moveTime.")
    }
}

object HeadlessPlayerMovementLogger : PlayerMovementListener {

    val LOGGER: Logger = LoggerFactory.getLogger(HeadlessPlayerMovementLogger::class.java)

    override fun onPlayerAppear(playerSnapshot: PlayerSnapshot) {
        LOGGER.info("Player '${playerSnapshot.nick} (${playerSnapshot.id})' entered the maze.")
    }

    override fun onPlayerVanish(playerSnapshot: PlayerSnapshot) {
        LOGGER.info("Player '${playerSnapshot.nick} (${playerSnapshot.id})' left the maze.")
    }

    override fun onPlayerStep(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        LOGGER.info("Player '${newPlayerSnapshot.nick} (${newPlayerSnapshot.id})' made a step forward.")
    }

    override fun onPlayerTurn(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        LOGGER.info("Player '${newPlayerSnapshot.nick} (${newPlayerSnapshot.id})' turned from '${oldPosition.viewDirection}' to '${newPlayerSnapshot.viewDirection}'.")
    }

    override fun onPlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        val collisionReason = when (causingPlayerId) {
            null -> ""
            newPlayerSnapshot.id -> " It was their own fault."
            else -> " It was the other player's fault ($causingPlayerId)."
        }
        val teleportReason: String = when (teleportType) {
            null -> ""
            TeleportType.TRAP -> " They ran into a trap."
            TeleportType.COLLISION -> " They collided with another player.$collisionReason"
        }
        LOGGER.info("Player '${newPlayerSnapshot.nick} (${newPlayerSnapshot.id})' was teleported away.$teleportReason")
    }
}

object HeadlessPlayerScoreLogger : ScoreChangeListener {
    val LOGGER: Logger = LoggerFactory.getLogger(HeadlessPlayerScoreLogger::class.java)

    override fun onScoreChange(oldScore: Int, newPlayerSnapshot: PlayerSnapshot) {
        val scoreDifference: Int = newPlayerSnapshot.score - oldScore
        val collectedBaitType: BaitType? = BaitType.Companion.byScore(scoreDifference)
        val scoreReason: String = when (collectedBaitType) {
            null -> ""
            BaitType.TRAP -> " They ran into a trap."
            else -> " They collected a ${collectedBaitType.baitName}."
        }
        LOGGER.info("Player '${newPlayerSnapshot.nick} ${newPlayerSnapshot.id}' made '$scoreDifference' points.$scoreReason")
    }

}