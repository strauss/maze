package mazegame.server_ktor.maze.commands

import de.dreamcube.mazegame.common.maze.Command
import mazegame.server_ktor.maze.MazeServer

abstract class ServerSideCommand(protected val mazeServer: MazeServer) : Command
