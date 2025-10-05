package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.common.maze.Command
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

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