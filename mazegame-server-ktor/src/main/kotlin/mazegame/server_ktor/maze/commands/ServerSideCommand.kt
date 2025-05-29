package mazegame.server_ktor.maze.commands

import mazegame.server_ktor.maze.MazeServer

abstract class ServerSideCommand(protected val mazeServer: MazeServer) : Command
