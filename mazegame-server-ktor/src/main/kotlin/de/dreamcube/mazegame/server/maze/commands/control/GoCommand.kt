package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand


/**
 * Command for generating baits if there are none. Sets the desired bait count to base bait count and generates baits.
 */
class GoCommand(mazeServer: MazeServer) : ServerSideCommand(mazeServer) {

    @Suppress("kotlin:S6518") // yeah ...
    override suspend fun execute() {
        mazeServer.desiredBaitCount.set(mazeServer.baseBaitCount)
        if (mazeServer.currentBaitCount.get() < mazeServer.desiredBaitCount.get()) {
            val baitGenerationMessages = mazeServer.fillBaits()
            mazeServer.sendToAllPlayers(*baitGenerationMessages.toTypedArray())
        }
    }

}