/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dreamcube.mazegame.ui

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.dreamcube.mazegame.common.api.*
import de.dreamcube.mazegame.common.maze.BaitType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class communicates with the server for sending server control commands using REST.
 */
class ServerCommandController(
    coroutineScope: CoroutineScope,
    private val serverAddress: String,
    private val serverPort: Int,
    private val serverPassword: String,
    private val gamePort: Int
) : CoroutineScope by CoroutineScope(coroutineScope.coroutineContext + SupervisorJob()) {
    companion object {

        private val LOGGER: Logger = LoggerFactory.getLogger(ServerCommandController::class.java)

        private const val USER_NAME = "master"

        /**
         * Creates an HTTP client that is supposed to be used for a single call.
         */
        private fun createDisposableHttpClient() = HttpClient(CIO) {
            this.expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }

        /**
         * This function retrieves the game information from the server. It requires no login information, so we can
         * place it in the companion object. Therefore, we don't need an instance of the class.
         */
        internal suspend fun queryForGameInformation(address: String, port: Int): List<ReducedServerInformationDto> {
            val httpAddress = "http://$address:$port/server"
            val httpClient = createDisposableHttpClient()

            httpClient.use {
                return it.get(httpAddress).body()
            }
        }

    }

    private var authToken: String? = null
    private val tokenMutex = Mutex()

    private val baseUrl
        get() = "http://$serverAddress:$serverPort"

    /**
     * This function performs a login or refreshes the token manually. If [autoRefresh] is set, a coroutine is launched
     * with a delay time depending on the TTL of the token, that automatically refreshes the token after a while.
     */
    suspend fun loginOrRefreshToken(autoRefresh: Boolean = true): String {
        val httpAddress = "$baseUrl/login"
        val httpClient: HttpClient = createDisposableHttpClient()
        val result: HttpResponse = httpClient.use { it.post(httpAddress) { basicAuth(USER_NAME, serverPassword) } }
        val receivedToken = result.body<JwtToken>()
        tokenMutex.withLock {
            authToken = receivedToken.token
        }
        LOGGER.info("Successfully logged in at server for server control.")


        // now we automatically refresh the token
        val waitTimeInMilliseconds = (receivedToken.expires - 1) * 1000L
        launch {
            delay(waitTimeInMilliseconds)
            if (autoRefresh) {
                LOGGER.info("Attempting token refresh for server control.")
                loginOrRefreshToken()
            } else {
                tokenMutex.withLock {
                    authToken = null
                    LOGGER.warn("Server control token expired!")
                }
            }
        }

        return receivedToken.token
    }

    internal suspend fun readToken(): String? = tokenMutex.withLock { authToken }

    /**
     * Double protection ... if we are not logged in, we automatically create a new token.
     */
    private suspend fun ensureLoggedIn(): String {
        val token: String? = readToken()
        if (token == null) {
            return loginOrRefreshToken()
        }
        return token
    }

    /**
     * Sends the "go" command. If the server is stopped, it will start generating baits again.
     */
    suspend fun go() {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/go"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

    /**
     * Sends the "clear" command without stopping the server. This will just clear the scores.
     */
    suspend fun clear() {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/clear"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use {
            it.post(httpAddress) {
                bearerAuth(token)
                url {
                    parameter("stop", false)
                }
            }
        }
    }

    /**
     * Sends the "stop" command. Depending on the [now] flag the server will stop immediately or just stop generating
     * new baits.
     */
    suspend fun stop(now: Boolean = false) {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/stop"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use {
            it.post(httpAddress) {
                bearerAuth(token)
                if (now) {
                    url {
                        parameter("now", now)
                    }
                }
            }
        }
    }

    /**
     * Changes the game speed to [speed].
     */
    suspend fun changeSpeed(speed: GameSpeed) {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/speed"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use {
            it.post(httpAddress) {
                bearerAuth(token)
                url {
                    parameter("speed", speed.shortName)
                }
            }
        }
    }

    /**
     * Transforms all baits to [BaitType].
     */
    suspend fun baitTransform(baitType: BaitType) {
        val suffix: String = when (baitType) {
            BaitType.FOOD -> "all-food"
            BaitType.COFFEE -> "all-coffee"
            BaitType.GEM -> "all-gems"
            BaitType.TRAP -> "all-traps"
        }
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/$suffix"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

    /**
     * Puts a new bait at the given location defined by [x] and [y].
     */
    suspend fun baitPut(baitType: BaitType, x: Int, y: Int) {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/put-bait"
        val httpClient: HttpClient = createDisposableHttpClient()
        val putBaitCommand = PutBaitCommandDto(baitType, x, y)
        httpClient.use {
            it.post(httpAddress) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(putBaitCommand)
            }
        }
    }

    /**
     * Starts a bait rush (temporarily more baits).
     */
    suspend fun baitRush() {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/bait-rush"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

    /**
     * Removes the player with [playerId] from the game.
     */
    suspend fun kill(playerId: Int) {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/kill/$playerId"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

    /**
     * Teleports the player with the given [playerId] to the location defined by [x] and [y].
     */
    suspend fun teleport(playerId: Int, x: Int, y: Int) {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/teleport"
        val httpClient: HttpClient = createDisposableHttpClient()
        val teleportCommand = TeleportCommandDto(playerId, x, y)
        httpClient.use {
            it.post(httpAddress) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(teleportCommand)
            }
        }
    }

    /**
     * Retrieves the player information for the player with the given [playerId].
     */
    suspend fun playerInformation(playerId: Int): PlayerInformationDto {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/info/player/$playerId"
        val httpClient: HttpClient = createDisposableHttpClient()
        return httpClient.use { it.get(httpAddress) { bearerAuth(token) }.body() }
    }

    /**
     * Retrieves the complete server information. It is used to retrieve the list of available strategy names for
     * server-sided bots.
     */
    suspend fun serverInformation(): ServerInformationDto {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/info"
        val httpClient: HttpClient = createDisposableHttpClient()
        return httpClient.use { it.get(httpAddress) { bearerAuth(token) } }.body()
    }

    /**
     * Spawns a new server-sided bot instance with the given [nick] as strategy name. The actual nickname is selected by
     * the server.
     */
    suspend fun spawn(nick: String) {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/spawn/$nick"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

}