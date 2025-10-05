package de.dreamcube.mazegame.client.maze.commands

/**
 * Interface for executing commands.
 */
interface Command {
    suspend fun execute()
}