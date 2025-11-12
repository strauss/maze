package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.PlayerSnapshot

/**
 * This command indicates a score change.
 */
class PlayerScoreCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    /**
     * The id of the affected player.
     */
    private val playerId: Int

    /**
     * The new [score] of the affected player.
     */
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

    /**
     * Triggers a score change event.
     */
    override suspend fun internalExecute() {
        mazeClient.players.changePlayerScore(playerId, score)
            ?.let { (oldScore, playerSnapshot): Pair<Int, PlayerSnapshot> ->
                mazeClient.eventHandler.fireScoreChange(oldScore, playerSnapshot)
            }
    }

}