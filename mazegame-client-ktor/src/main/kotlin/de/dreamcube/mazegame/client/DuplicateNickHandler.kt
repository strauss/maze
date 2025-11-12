package de.dreamcube.mazegame.client

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.events.ErrorInfoListener
import de.dreamcube.mazegame.common.maze.InfoCode
import kotlinx.coroutines.runBlocking

/**
 * Simple handler for dealing with duplicate nicknames. Uses the built-in retry mechanism for logging in.
 */
class DuplicateNickHandler(val mazeClient: MazeClient) : ErrorInfoListener {
    private var loginIteration = 0

    override fun onServerError(infoCode: InfoCode) {
        when (infoCode) {
            InfoCode.DUPLICATE_NICK -> {
                loginIteration += 1
                runBlocking { mazeClient.internalConnect(loginIteration) }
            }

            else -> {
                // ignore
            }
        }
    }
}