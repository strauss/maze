package de.dreamcube.mazegame.client

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val config = MazeClientConfigurationDto("localhost", 12345, "aimless")
    val mazeClient = MazeClient(config)
    val start: Deferred<Unit> = mazeClient.start()
    runBlocking {
        launch {
            delay(5000L)
            mazeClient.logout()
        }
        start.await()
    }
}