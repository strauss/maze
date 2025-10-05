package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.ServerSideCommand


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