package mazegame.server_ktor.maze.commands.control

import mazegame.server_ktor.maze.ClientConnection
import mazegame.server_ktor.maze.MazeServer
import mazegame.server_ktor.maze.commands.ServerSideCommand

class KillCommand(mazeServer: MazeServer, val clientConnection: ClientConnection) : ServerSideCommand(mazeServer) {

    override suspend fun execute() {
        if (clientConnection.isAutoTrapeater) {
            mazeServer.autoTrapeaterHandler.despawn()
        } else if (clientConnection.isFrenzyBot) {
            mazeServer.frenzyHandler.despawn()
        }
        clientConnection.stop()
    }
}