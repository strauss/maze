package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.common.maze.PlayerPositionChangeReason
import de.dreamcube.mazegame.common.maze.TeleportType
import de.dreamcube.mazegame.common.maze.ViewDirection

class PlayerPosCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    private val id: Int
    private val x: Int
    private val y: Int
    private val viewDirection: ViewDirection
    private val reason: PlayerPositionChangeReason
    private val teleportType: TeleportType?
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

    override suspend fun internalExecute() {
        mazeClient.players.changePlayerPosition(id, x, y, viewDirection, reason)?.let {
            val (oldSnapshot: PlayerSnapshot, newSnapshot: PlayerSnapshot) = it
            when (reason) {
                PlayerPositionChangeReason.MOVE -> mazeClient.eventHandler.firePlayerStep(oldSnapshot.position, newSnapshot)
                PlayerPositionChangeReason.TURN -> mazeClient.eventHandler.firePlayerTurn(oldSnapshot.position, newSnapshot)
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