package de.dreamcube.mazegame.client

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

fun main() {
    val config = MazeClientConfigurationDto("localhost", 12345, "aimless")
    val start: Deferred<Unit> = MazeClient(config).start()
    runBlocking { start.await() }
}