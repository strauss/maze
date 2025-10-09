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
    private val otherPlayerId: Int?

    override val okay: Boolean

    init {
        if (commandWithParameters.size < 6) {
            id = -1
            x = -1
            y = -1
            viewDirection = ViewDirection.random()
            reason = PlayerPositionChangeReason.VANISH
            teleportType = null
            otherPlayerId = null
            okay = false
        } else {
            id = commandWithParameters[1].toInt()
            x = commandWithParameters[2].toInt()
            y = commandWithParameters[3].toInt()
            viewDirection = ViewDirection.fromShortName(commandWithParameters[4])
            reason = PlayerPositionChangeReason.fromShortName(commandWithParameters[5])
            teleportType = if (reason == PlayerPositionChangeReason.TELEPORT && commandWithParameters.size >= 7)
                TeleportType.fromShortName(commandWithParameters[6]) else null
            otherPlayerId = if (teleportType != null && commandWithParameters.size >= 8)
                commandWithParameters[7].toInt() else null
            okay = true
        }
    }

    override suspend fun internalExecute() {
        mazeClient.players.changePlayerPosition(id, x, y, viewDirection, reason)?.let {
            val (oldSnapshot: PlayerSnapshot, newSnapshot: PlayerSnapshot) = it
            // TODO: send event based on reason
        }

    }
}