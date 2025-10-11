package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.client.maze.events.PlayerMovementListener
import de.dreamcube.mazegame.client.maze.events.ScoreChangeListener
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.TeleportType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HeadlessGameEventLogger : PlayerConnectionListener, PlayerMovementListener, ScoreChangeListener {

    val LOGGER: Logger = LoggerFactory.getLogger(HeadlessGameEventLogger::class.java)

    override fun onPlayerLogin(playerId: Int, nick: String) {
        LOGGER.info("Player '$nick' with id '$playerId' logged in.")
    }

    override fun onPlayerLogout(playerId: Int) {
        LOGGER.info("Player with id '$playerId' logged out.")
    }

    override fun onPlayerAppear(player: PlayerSnapshot) {
        LOGGER.info("Player '${player.nick}(${player.id})' entered the maze.")
    }

    override fun onPlayerVanish(player: PlayerSnapshot) {
        LOGGER.info("Player '${player.nick}(${player.id})' left the maze.")
    }

    override fun onPlayerStep(
        oldPosition: PlayerSnapshot,
        newPosition: PlayerSnapshot
    ) {
        LOGGER.info("Player '${oldPosition.nick}(${oldPosition.id})' made a step forward.")
    }

    override fun onPlayerTurn(
        oldPosition: PlayerSnapshot,
        newPosition: PlayerSnapshot
    ) {
        LOGGER.info("Player '${oldPosition.nick}(${oldPosition.id})' turned from '${oldPosition.viewDirection}' to '${newPosition.viewDirection}'.")
    }

    override fun onPlayerTeleport(
        oldPosition: PlayerSnapshot,
        newPosition: PlayerSnapshot,
        teleportType: TeleportType?,
        otherPlayerId: Int?
    ) {
        val collisionReason = when (otherPlayerId) {
            null -> ""
            oldPosition.id -> " It was their own fault."
            else -> " It was the other player's fault ($otherPlayerId)."
        }
        val teleportReason: String = when (teleportType) {
            null -> ""
            TeleportType.TRAP -> " They ran into a trap."
            TeleportType.COLLISION -> " They collided with another player. $collisionReason"
        }
        LOGGER.info("Player '${oldPosition.nick}(${oldPosition.id})' was teleported away.$teleportReason")
    }

    override fun onScoreChange(playerId: Int, oldScore: Int, newScore: Int) {
        val scoreDifference: Int = newScore - oldScore
        val collectedBaitType: BaitType? = BaitType.byScore(scoreDifference)
        val scoreReason: String = when (collectedBaitType) {
            null -> ""
            BaitType.TRAP -> " They ran into a trap."
            else -> " They collected a ${collectedBaitType.baitName}."
        }
        LOGGER.info("Player with id '$playerId' made '$scoreDifference' points.$scoreReason")
    }

}