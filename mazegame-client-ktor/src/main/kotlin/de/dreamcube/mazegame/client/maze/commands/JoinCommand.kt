package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.view
import de.dreamcube.mazegame.common.maze.Player

class JoinCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    private val playerId: Int
    private val nick: String
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 3) {
            playerId = -1
            nick = ""
            okay = false
        } else {
            playerId = commandWithParameters[1].toInt()
            nick = commandWithParameters[2]
            okay = true
        }
    }

    override suspend fun internalExecute() {
        val newPlayer = Player(playerId, nick)
        val success: Boolean = mazeClient.players.addPlayer(newPlayer)
        if (success) {
            val newPlayerView = newPlayer.view()
            val newPlayerSnapshot = newPlayerView.takeSnapshot()
            mazeClient.eventHandler.firePlayerLogin(newPlayerSnapshot)
            if (playerId == mazeClient.id) {
                mazeClient.ownPlayer = newPlayerView
                mazeClient.eventHandler.fireOwnPlayerLogin(newPlayerSnapshot)
            }
        }
    }
}