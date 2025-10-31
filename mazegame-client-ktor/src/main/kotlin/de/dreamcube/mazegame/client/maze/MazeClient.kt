package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.commands.ServerCommandParser
import de.dreamcube.mazegame.client.maze.events.EventHandler
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.common.maze.CommandExecutor
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.common.maze.PROTOCOL_VERSION
import de.dreamcube.mazegame.common.maze.sanitizeAsChatMessage
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

    lateinit var strategy: Strategy
        private set

    val serverAddress
        get() = clientConfiguration.serverAddress

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

    internal val baits = BaitCollection()
    internal val players = PlayerCollection()
    lateinit var ownPlayer: PlayerView
    private val ownPlayerInitialized: Boolean
        get() = this::ownPlayer.isInitialized
    val ownPlayerSnapshot: PlayerSnapshot
        get() = runBlocking { players.getPlayerSnapshot(ownPlayer.id)!! }

    private val selector = SelectorManager(Dispatchers.IO)

    /**
     * Starts the client ... Java edition.
     */
    @Suppress("kotlin:S6508") // This function is intended for Java callers and therefore requires Void instead of Unit
    fun startAndWait(): CompletableFuture<Void> = start().asCompletableFuture().thenApply { null }

    /**
     * Starts the client ... Kotlin edition.
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

    val isLoggedIn
        get() = status in loggedInStates

    /**
     * Function for logging out of the game.
     */
    suspend fun logout() {
        if (isLoggedIn) {
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

    internal suspend fun sendMessage(message: Message) {
        try {
            outgoingMessages.send(message)
        } catch (_: ClosedSendChannelException) {
            LOGGER.error("Outgoing channel was already closed.")
        }
    }

    internal suspend fun connect(serverProtocolVersion: Int) {
        if (serverProtocolVersion == PROTOCOL_VERSION) {
            internalConnect()
        }
    }

    internal suspend fun internalConnect(iteration: Int = 0) {
        val nameSuffix = if (iteration == 0) "" else iteration.toString()
        sendMessage(createHelloMessage("${clientConfiguration.displayName}$nameSuffix"))
    }

    internal suspend fun loggedIn(id: Int) {
        if (status == ConnectionStatus.CONNECTED && id > 0) {
            this.id = id
            status = ConnectionStatus.LOGGED_IN
            sendMessage(createRequestMazeMessage())
        }
    }

    internal fun initializeMaze(width: Int, height: Int, mazeLines: List<String>) {
        if (status == ConnectionStatus.LOGGED_IN) {
            eventHandler.fireMazeReceived(width, height, mazeLines)
            status = ConnectionStatus.SPECTATING
        }
    }

    internal suspend fun onReady() {
        if (status == ConnectionStatus.SPECTATING) {
            if (!ownPlayerInitialized) {
                LOGGER.error("Own player did not join the game: leaving the game again!")
                return logout()
            }
            status = ConnectionStatus.PLAYING
        }
        if (status == ConnectionStatus.PLAYING) {
            strategy.makeNextMove()
        }
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
        return runBlocking { players.elements() }
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