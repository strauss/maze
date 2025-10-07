package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.BaitPositionChange
import de.dreamcube.mazegame.common.maze.BaitType

class BaitPosCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    val x: Int
    val y: Int
    val type: BaitType
    val reason: BaitPositionChange

    init {
        if (commandWithParameters.size < 5) {
            TODO("ERROR")
        }
        x = commandWithParameters[1].toInt()
        y = commandWithParameters[2].toInt()
        type = BaitType.byName(commandWithParameters[3])
        reason = BaitPositionChange.byName(commandWithParameters[4])
    }

    override suspend fun execute() {
        when (reason) {
            BaitPositionChange.COLLECTED -> {
                mazeClient.baits.getBait(x, y)?.let { bait: Bait ->
                    val success: Boolean = mazeClient.baits.removeBait(bait)
                    if (success) {
                        // TODO: send event
                    }
                }
            }

            BaitPositionChange.GENERATED -> {
                val bait = Bait(type, x, y)
                val success: Boolean = mazeClient.baits.addBait(bait)
                if (success) {
                    // TODO: send event
                }
            }
        }
    }
}