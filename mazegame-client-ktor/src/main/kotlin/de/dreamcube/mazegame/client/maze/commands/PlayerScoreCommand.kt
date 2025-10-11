package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.PlayerSnapshot

class PlayerScoreCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    private val playerId: Int
    private val score: Int
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 3) {
            playerId = -1
            score = -1
            okay = false
        } else {
            playerId = commandWithParameters[1].toInt()
            score = commandWithParameters[2].toInt()
            okay = true
        }
    }

    override suspend fun internalExecute() {
        mazeClient.players.changePlayerScore(playerId, score)?.let { (oldScore, playerSnapshot): Pair<Int, PlayerSnapshot> ->
            mazeClient.eventHandler.fireScoreChange(oldScore, playerSnapshot)
        }
    }

}