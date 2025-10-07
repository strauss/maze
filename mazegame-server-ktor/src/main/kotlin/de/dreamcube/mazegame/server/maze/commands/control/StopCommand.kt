package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand

/**
 * Command for interrupting the game by withdrawing all baits. If [now] is set, all baits are withdrawn immediately. If not, only the traps are
 * withdrawn and the bots have to collect them for the final stop, but no new baits will be generated.
 */
class StopCommand(mazeServer: MazeServer, val now: Boolean = false) : ServerSideCommand(mazeServer) {

    override suspend fun execute() {
        mazeServer.desiredBaitCount.set(0)
        val messages: List<Message> = if (now) mazeServer.withdrawBaits() else mazeServer.withdrawBaits { it == BaitType.TRAP }
        if (messages.isNotEmpty()) {
            mazeServer.sendToAllPlayers(*messages.toTypedArray())
        }
        mazeServer.autoTrapeaterHandler.despawn()
    }
}