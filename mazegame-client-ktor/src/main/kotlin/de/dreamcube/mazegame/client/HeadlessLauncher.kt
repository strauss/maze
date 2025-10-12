package de.dreamcube.mazegame.client

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOGGER: Logger = LoggerFactory.getLogger("main")

fun main() {
    // Bot-Strategien laden
    Strategy.scanAndAddStrategiesBlocking()
    LOGGER.info("Found strategies: ${Strategy.getStrategyNamesBlocking()}")
    val strategyName = "trapeater"
    val config = MazeClientConfigurationDto("localhost", 12345, strategyName)

    val mazeClient = MazeClient(config)
    mazeClient.eventHandler.addEventListener(HeadlessChatDisplay)
    mazeClient.eventHandler.addEventListener(HeadlessErrorDisplay)
    mazeClient.eventHandler.addEventListener(HeadlessErrorHandler(mazeClient))
    mazeClient.eventHandler.addEventListener(HeadlessPlayerConnectionLogger)
    mazeClient.eventHandler.addEventListener(HeadlessPlayerScoreLogger)

    mazeClient.eventHandler.fireClientInfo("The game is about to start!")
    try {
        val start: Deferred<Unit> = mazeClient.start()
        runBlocking {
            launch {
                delay(5000L)
                mazeClient.logout()
            }
            start.await()
        }
    } catch (ex: Exception) {
        LOGGER.error("Error while starting the client: ${ex.message}", ex)
    }
}