package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.PlayerSnapshot

class LeaveCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    private val playerId: Int
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            playerId = -1
            okay = false
        } else {
            playerId = commandWithParameters[1].toInt()
            okay = true
        }
    }

    override suspend fun internalExecute() {
        val removedPlayer: PlayerSnapshot? = mazeClient.players.removePlayerById(playerId)
        removedPlayer?.let { player -> mazeClient.eventHandler.firePlayerLogout(player) }
    }
}