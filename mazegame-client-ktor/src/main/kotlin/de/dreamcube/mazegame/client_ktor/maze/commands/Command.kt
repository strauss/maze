package de.dreamcube.mazegame.client_ktor.maze.commands

/**
 * Interface for executing commands.
 */
interface Command {
    suspend fun execute()
}