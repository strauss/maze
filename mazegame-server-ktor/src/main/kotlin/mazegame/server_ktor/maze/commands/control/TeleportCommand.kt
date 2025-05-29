package mazegame.server_ktor.maze.commands.control

import kotlinx.coroutines.CompletableDeferred
import mazegame.server_ktor.maze.ClientConnection
import mazegame.server_ktor.maze.MazeServer
import mazegame.server_ktor.maze.Message
import mazegame.server_ktor.maze.commands.Command
import mazegame.server_ktor.maze.createServerInfoMessage

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

