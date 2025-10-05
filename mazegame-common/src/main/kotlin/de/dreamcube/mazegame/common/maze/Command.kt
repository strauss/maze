package de.dreamcube.mazegame.common.maze

/**
 * Interface for executing commands.
 */
fun interface Command {
    suspend fun execute()
}