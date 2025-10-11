package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.BaitPositionChange
import de.dreamcube.mazegame.common.maze.BaitType

class BaitPosCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {

    private val x: Int
    private val y: Int
    private val type: BaitType
    private val reason: BaitPositionChange
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 5) {
            x = -1
            y = -1
            type = BaitType.TRAP
            reason = BaitPositionChange.GENERATED
            okay = false
        } else {
            x = commandWithParameters[1].toInt()
            y = commandWithParameters[2].toInt()
            type = BaitType.byName(commandWithParameters[3])
            reason = BaitPositionChange.byName(commandWithParameters[4])
            okay = true
        }
    }

    override suspend fun internalExecute() {
        when (reason) {
            BaitPositionChange.COLLECTED -> {
                mazeClient.baits.getBait(x, y)?.let { bait: Bait ->
                    val success: Boolean = mazeClient.baits.removeBait(bait)
                    if (success) {
                        mazeClient.eventHandler.fireBaitVanished(bait)
                    }
                }
            }

            BaitPositionChange.GENERATED -> {
                val bait = Bait(type, x, y)
                val success: Boolean = mazeClient.baits.addBait(bait)
                if (success) {
                    mazeClient.eventHandler.fireBaitAppeared(bait)
                }
            }
        }
    }
}