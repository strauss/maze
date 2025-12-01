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

package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.commands.ServerCommandParser
import de.dreamcube.mazegame.client.maze.events.EventHandler
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.client.maze.strategy.Strategy.Companion.flavorText
import de.dreamcube.mazegame.common.maze.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.future.asCompletableFuture
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * Central maze client class.
 */
class MazeClient @JvmOverloads constructor(
    internal val clientConfiguration: MazeClientConfigurationDto,
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val NO_ID: Int = -1
        private val LOGGER: Logger = LoggerFactory.getLogger(MazeClient::class.java)

        private val loggedInStates =
            setOf(ConnectionStatus.LOGGED_IN, ConnectionStatus.SPECTATING, ConnectionStatus.PLAYING)
    }

    /**
     * The reference to the strategy object. It is initialized, as soon as the connection is about to be established.
     */
    lateinit var strategy: Strategy
        private set

    /**
     * The server address.
     */
    val serverAddress
        get() = clientConfiguration.serverAddress

    /**
     * The server port.
     */
    val serverPort
        get() = clientConfiguration.serverPort

    /**
     * The socket for the connection.
     */
    private lateinit var socket: Socket

    /**
     * The current status of the connection.
     */
    var status: ConnectionStatus = ConnectionStatus.NOT_CONNECTED
        private set(value) {
            val oldStatus = field
            field = value
            if (oldStatus != value) {
                eventHandler.fireConnectionStatusChange(oldStatus, value)
            }
        }

    /**
     * The id from the server.
     */
    var id = NO_ID
        private set

    /**
     * The current game speed ... can be changed by the server. 150 is the historical default game speed.
     */
    var gameSpeed: Int = 150
        internal set

    /**
     * Channel for outgoing messages.
     */
    private val outgoingMessages = Channel<Message>(Channel.UNLIMITED)

    /**
     * The client command executor.
     */
    private val commandExecutor = CommandExecutor(scope)

    /**
     * The command Parser.
     */
    private val commandParser = ServerCommandParser(scope, this@MazeClient, commandExecutor)

    /**
     * The event handler. Each client has its own, or it would get very messy.
     */
    val eventHandler = EventHandler()

    private lateinit var readJob: Job
    private lateinit var writeJob: Job

    /**
     * The internal collection of baits.
     */
    internal val baits = BaitCollection()

    /**
     * The internal collection of players.
     */
    internal val players = PlayerCollection()

    /**
     * The reference to the own player object. It is initialized during [status] [ConnectionStatus.LOGGED_IN] right
     * after the maze data was received. Only in status [ConnectionStatus.SPECTATING] or [ConnectionStatus.PLAYING] you
     * can be sure this reference exists.
     */
    lateinit var ownPlayer: PlayerView

    /**
     * Checks, if [ownPlayer] has already been initialized.
     */
    private val ownPlayerInitialized: Boolean
        get() = this::ownPlayer.isInitialized

    /**
     * Takes a snapshot of the own player object.
     */
    val ownPlayerSnapshot: PlayerSnapshot
        get() = runBlocking { players.getPlayerSnapshot(ownPlayer.id)!! }

    private val selector = SelectorManager(Dispatchers.IO)

    /**
     * Starts the client ... Java edition.
     */
    @Suppress("kotlin:S6508") // This function is intended for Java callers and therefore requires Void instead of Unit
    fun startAndWait(): CompletableFuture<Void> = start().asCompletableFuture().thenApply { null }

    /**
     * Starts the client ... Kotlin edition. Creates the strategy object and establishes the connection in a coroutine.
     */
    fun start(): Deferred<Unit> {
        // We have to get the strategy somewhere ... this is one way to do it.
        strategy = Strategy.createStrategyBlocking(clientConfiguration.strategyName)
            ?: throw ClassNotFoundException("Could not find strategy with name '${clientConfiguration.strategyName}'")
        strategy.initClient(this)
        val result = CompletableDeferred<Unit>()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                socket = aSocket(selector).tcp().connect(serverAddress, serverPort)
            } catch (ex: Exception) {
                LOGGER.error("Connection error: ${ex.message}", ex)
                status = ConnectionStatus.DEAD
                result.completeExceptionally(ex)
                return@launch
            }
            commandExecutor.start()
            commandParser.start()
            writeJob = launch { writeLoop() }
            readJob = scope.launch { readLoop(scope) }
            status = ConnectionStatus.CONNECTED

            readJob.join()
            stop(true)
            result.complete(Unit)
        }
        return result
    }

    /**
     * Indicates if the client is logged in.
     */
    val isLoggedIn
        get() = status in loggedInStates

    /**
     * Function for logging out of the game.
     */
    suspend fun logout() {
        if (isLoggedIn) {
            strategy.beforeGoodbye()
            LOGGER.info("Logging out...")
            sendMessage(createByeMessage())
        } else {
            LOGGER.warn("Not logged in.")
            stop(true)
        }
    }

    /**
     * Function for logging out of the game ... Java style.
     */
    fun logoutBlocking() {
        runBlocking { logout() }
    }

    /**
     * Initializes a clean logout. Is also called whenever the server terminates the connection. The call is idempotent
     * because it reacts to the [status] [ConnectionStatus.DYING] and [ConnectionStatus.DEAD], meaning if the client
     * is already dying or dead, it is not tried to kill it again.
     */
    internal suspend fun stop(clientSide: Boolean) {
        if (status == ConnectionStatus.DEAD || status == ConnectionStatus.DYING) {
            return
        }
        try {
            status = ConnectionStatus.DYING
            if (clientSide) {
                LOGGER.info("Terminating the connection...")
            } else {
                LOGGER.warn("The server terminated the connection!")
            }
            outgoingMessages.close()
            if (this::writeJob.isInitialized) {
                writeJob.join()
            }
            status = ConnectionStatus.DEAD
            if (this::socket.isInitialized) {
                LOGGER.info("Connection closed: '{}'", socket.remoteAddress)
                socket.close()
            } else {
                LOGGER.warn("Client terminated before connection to '${serverAddress}:${serverPort}' was established.")
            }
        } finally {
            selector.close()
        }
    }

    /**
     * Internal read loop. Reads directly from the network socket's [ByteReadChannel].
     */
    private suspend fun readLoop(scope: CoroutineScope) {
        try {
            val input: ByteReadChannel = socket.openReadChannel()
            while (scope.isActive) {
                val line = input.readUTF8Line() ?: break
                LOGGER.debug("Received line: '{}'", line)
                commandParser.receive(line)
            }
        } catch (_: ClosedByteChannelException) {
            // do nothing
        }
    }

    /**
     * Internal write loop. Writes directly to the network socket's [ByteWriteChannel].
     */
    private suspend fun writeLoop() {
        try {
            val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = false)
            for (message: Message in outgoingMessages) {
                if (message.msg.isNotEmpty()) {
                    output.writeStringUtf8("${message.msg}\n")
                    LOGGER.debug("Sent message: '{}'", message.msg)
                }
                if (message.lastMessage) {
                    output.flush()
                }
            }
        } catch (_: ClosedByteChannelException) {
            // do nothing
        }
    }

    /**
     * Sends a [Message] to the server.
     */
    internal suspend fun sendMessage(message: Message) {
        try {
            outgoingMessages.send(message)
        } catch (_: ClosedSendChannelException) {
            LOGGER.error("Outgoing channel was already closed.")
        }
    }

    /**
     * If the protocol version matches, the connection is established.
     */
    internal suspend fun connect(serverProtocolVersion: Int) {
        if (serverProtocolVersion == PROTOCOL_VERSION) {
            internalConnect()
        }
        // TODO: abort with an error message
    }

    /**
     * Connects to the server. The optional iteration parameter is used for attaching a number to the nickname, if the
     * number is bigger than 0. Can be used for a simple retry mechanism.
     */
    internal suspend fun internalConnect(iteration: Int = 0) {
        val nameSuffix = if (iteration == 0) "" else iteration.toString()
        val flavor: String? =
            if (clientConfiguration.withFlavor) clientConfiguration.strategyName.flavorText() else null
        sendMessage(createHelloMessage("${clientConfiguration.displayName}$nameSuffix", flavor))
    }

    /**
     * Changes the [status] from [ConnectionStatus.CONNECTED] to [ConnectionStatus.LOGGED_IN]. Sends the "MAZ?" message
     * to the server for requesting the maze data.
     */
    internal suspend fun loggedIn(id: Int) {
        if (status == ConnectionStatus.CONNECTED && id > 0) {
            this.id = id
            status = ConnectionStatus.LOGGED_IN
            sendMessage(createRequestMazeMessage())
        }
    }

    /**
     * Processes the receiving of the maze by changing the [status] to [ConnectionStatus.SPECTATING] and firing the maze
     * received event. Only works, if the client is in [status] [ConnectionStatus.LOGGED_IN].
     */
    internal fun initializeMaze(width: Int, height: Int, mazeLines: List<String>) {
        if (status == ConnectionStatus.LOGGED_IN) {
            eventHandler.fireMazeReceived(width, height, mazeLines)
            status = ConnectionStatus.SPECTATING
        }
    }

    /**
     * Is executed whenever the client receives a "RDY." command. When being called for the first time, the [status]
     * changes from [ConnectionStatus.SPECTATING] to [ConnectionStatus.PLAYING].
     */
    internal suspend fun onReady() {
        if (status == ConnectionStatus.SPECTATING) {
            if (!ownPlayerInitialized) {
                LOGGER.error("Own player did not join the game: leaving the game again!")
                return logout()
            }
            status = ConnectionStatus.PLAYING
        } // no "else" here, because the bot would then never start moving. It would mean "ignore the first RDY!".
        if (status == ConnectionStatus.PLAYING) {
            strategy.makeNextMove()
        }
    }

    /**
     * Sets the offset to the current score. This is done in the client for better score comparison after new players
     * have joined the game. Should only be called by the UI.
     */
    suspend fun softResetScores() {
        players.softResetAllPlayerScores()
    }

    /**
     * Gets a thread- and coroutine-safe snapshot of all [Bait]s. This function is intended for Kotlin callers.
     */
    suspend fun getBaits(): List<Bait> {
        return baits.elements()
    }

    /**
     * Gets a thread- and coroutine-safe snapshot of all [Bait]s. This function/method is intended for Java callers.
     */
    @JvmName("getBaits")
    fun getBaitsBlocking(): List<Bait> {
        return runBlocking { baits.elements() }
    }

    /**
     * Retrieves the bait at [x]/[y] or null if there is none.
     */
    suspend fun getBaitAt(x: Int, y: Int): Bait? = baits.getBait(x, y)

    /**
     * Gets a thread- and coroutine-safe snapshot of all players as list of [PlayerSnapshot]. This function is intended for Kotlin callers.
     */
    suspend fun getPlayers(): List<PlayerSnapshot> {
        return players.elements()
    }

    /**
     * Gets a thread- and coroutine-safe snapshot of all players as list of [PlayerSnapshot]. This function/method is intended for Java callers.
     */
    @JvmName("getPlayers")
    fun getPlayersBlocking(): List<PlayerSnapshot> {
        return runBlocking { getPlayers() }
    }

    /**
     * Sends a text message to all players (Java edition).
     */
    @JvmName("broadcast")
    fun broadcastBlocking(message: String) {
        runBlocking { broadcast(message) }
    }

    /**
     * Sends a text message to all players (Kotlin edition).
     */
    suspend fun broadcast(message: String) {
        sendMessage(createChatMessage(message.sanitizeAsChatMessage()))
    }

    /**
     * Sends a text message to all players (Java edition).
     */
    @JvmName("whisper")
    fun whisperBlocking(message: String, receiverPlayerId: Int) {
        runBlocking { whisper(message, receiverPlayerId) }
    }

    /**
     * Sends a whisper message to the player with id [receiverPlayerId] (Kotlin edition).
     */
    suspend fun whisper(message: String, receiverPlayerId: Int) {
        sendMessage(createWhisperMessage(message.sanitizeAsChatMessage(), receiverPlayerId))
    }

}