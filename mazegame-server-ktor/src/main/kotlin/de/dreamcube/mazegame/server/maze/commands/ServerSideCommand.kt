package de.dreamcube.mazegame.server.maze.commands

import de.dreamcube.mazegame.common.maze.Command
import de.dreamcube.mazegame.server.maze.MazeServer

abstract class ServerSideCommand(protected val mazeServer: MazeServer) : Command
