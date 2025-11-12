package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.common.maze.PlayerPositionChangeReason
import de.dreamcube.mazegame.common.maze.TeleportType
import de.dreamcube.mazegame.common.maze.ViewDirection

/**
 * This command is received whenever a player changed.
 */
class PlayerPosCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * The id of the affected player.
     */
    private val id: Int

    /**
     * The new [x] coordinate of the player.
     */
    private val x: Int

    /**
     * The new [y] coordinate of the player.
     */
    private val y: Int

    /**
     * The new [ViewDirection] of the player.
     */
    private val viewDirection: ViewDirection

    /**
     * The [reason] for the change. See [PlayerPositionChangeReason] for details.
     */
    private val reason: PlayerPositionChangeReason

    /**
     * The type of teleportation, if the [reason] is [PlayerPositionChangeReason.TELEPORT]. See [TeleportType] for
     * details.
     */
    private val teleportType: TeleportType?

    /**
     * If the [reason] was [PlayerPositionChangeReason.TELEPORT] and the [teleportType] was [TeleportType.COLLISION],
     * this field contains the id of the causing player. This can either be another player or the own player.
     */
    private val causingPlayerId: Int?

    override val okay: Boolean

    init {
        if (commandWithParameters.size < 6) {
            id = -1
            x = -1
            y = -1
            viewDirection = ViewDirection.random()
            reason = PlayerPositionChangeReason.VANISH
            teleportType = null
            causingPlayerId = null
            okay = false
        } else {
            id = commandWithParameters[1].toInt()
            x = commandWithParameters[2].toInt()
            y = commandWithParameters[3].toInt()
            viewDirection = ViewDirection.fromShortName(commandWithParameters[4])
            reason = PlayerPositionChangeReason.fromShortName(commandWithParameters[5])
            teleportType = if (reason == PlayerPositionChangeReason.TELEPORT && commandWithParameters.size > 6)
                TeleportType.fromShortName(commandWithParameters[6]) else null
            causingPlayerId = if (teleportType != null && commandWithParameters.size > 7)
                commandWithParameters[7].toInt() else null
            okay = true
        }
    }

    /**
     * Depending on the type of player position change, one of the following events is triggered:
     * - Player step
     * - Player turn
     * - Player teleport
     * - Player appear
     * - Player vanish
     */
    override suspend fun internalExecute() {
        mazeClient.players.changePlayerPosition(id, x, y, viewDirection, reason)?.let {
            val (oldSnapshot: PlayerSnapshot, newSnapshot: PlayerSnapshot) = it
            when (reason) {
                PlayerPositionChangeReason.MOVE -> mazeClient.eventHandler.firePlayerStep(
                    oldSnapshot.position,
                    newSnapshot
                )

                PlayerPositionChangeReason.TURN -> mazeClient.eventHandler.firePlayerTurn(
                    oldSnapshot.position,
                    newSnapshot
                )

                PlayerPositionChangeReason.TELEPORT -> mazeClient.eventHandler.firePlayerTeleport(
                    oldSnapshot.position,
                    newSnapshot,
                    teleportType,
                    causingPlayerId
                )

                PlayerPositionChangeReason.APPEAR -> mazeClient.eventHandler.firePlayerAppear(newSnapshot)
                PlayerPositionChangeReason.VANISH -> mazeClient.eventHandler.firePlayerVanish(oldSnapshot)
            }
        }

    }
}