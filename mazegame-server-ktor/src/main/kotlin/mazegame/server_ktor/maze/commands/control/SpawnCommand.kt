package mazegame.server_ktor.maze.commands.control

import de.dreamcube.mazegame.common.maze.Command
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import mazegame.server_ktor.maze.ClientConnection
import mazegame.server_ktor.maze.MazeServer

class SpawnCommand(val mazeServer: MazeServer, val nick: String) : Command {
    val reply: CompletableDeferred<ClientConnection?> = CompletableDeferred()

    override suspend fun execute() {
        try {
            mazeServer.scope.launch {
                val clientConnection: ClientConnection? = mazeServer.spawnServerSideBot(nick)
                reply.complete(clientConnection)
            }
        } catch (e: Throwable) {
            reply.completeExceptionally(e)
            throw e
        }
    }
}