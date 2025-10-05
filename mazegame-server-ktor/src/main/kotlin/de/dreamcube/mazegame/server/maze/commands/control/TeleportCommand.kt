package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.common.maze.Command
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.CompletableDeferred

class TeleportCommand(val mazeServer: MazeServer, val clientConnection: ClientConnection, val newX: Int, val newY: Int) : Command {
    val reply: CompletableDeferred<OccupationResult> = CompletableDeferred()

    override suspend fun execute() {
        try {
            val (result: OccupationResult, message: Message?) = mazeServer.teleportPlayer(clientConnection.player, newX, newY)
            if (result == OccupationResult.SUCCESS && message != null) {
                clientConnection.sendMessage(createServerInfoMessage("You have been teleported by a higher power!").thereIsMore())
                mazeServer.sendToAllPlayers(message)
            }
            reply.complete(result)
        } catch (e: Throwable) {
            reply.completeExceptionally(e)
            throw e
        }
    }
}

