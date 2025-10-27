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

        private fun createDisposableHttpClient() = HttpClient(CIO) {
            this.expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }

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
                authToken = null
                LOGGER.warn("Server control token expired!")
            }
        }

        return receivedToken.token
    }

    internal suspend fun readToken(): String? = tokenMutex.withLock { authToken }

    private suspend fun ensureLoggedIn(): String {
        val token: String? = readToken()
        if (token == null) {
            return loginOrRefreshToken()
        }
        return token
    }

    suspend fun go() {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/go"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

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

    suspend fun baitRush() {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/bait-rush"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

    suspend fun kill(playerId: Int) {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/control/kill/$playerId"
        val httpClient: HttpClient = createDisposableHttpClient()
        httpClient.use { it.post(httpAddress) { bearerAuth(token) } }
    }

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

    suspend fun playerInformation(playerId: Int): PlayerInformationDto {
        val token: String = ensureLoggedIn()
        val httpAddress = "$baseUrl/server/$gamePort/info/player/$playerId"
        val httpClient: HttpClient = createDisposableHttpClient()
        return httpClient.use { it.get(httpAddress) { bearerAuth(token) }.body() }
    }

}