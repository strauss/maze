package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.Player

class JoinCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    private val playerId: Int
    private val nick: String

    init {
        if (commandWithParameters.size < 3) {
            TODO("ERROR")
        }
        playerId = commandWithParameters[1].toInt()
        nick = commandWithParameters[2]
    }

    override suspend fun execute() {
        val newPlayer = Player(playerId, nick)
        val success: Boolean = mazeClient.players.addPlayer(newPlayer)
        if (success) {
            // TODO: send event
        }
    }
}