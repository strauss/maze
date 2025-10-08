package de.dreamcube.mazegame.server.maze

import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.common.maze.Player
import de.dreamcube.mazegame.server.maze.commands.client.createCommand
import de.dreamcube.mazegame.server.maze.delay_compensation.DelayCompensator
import de.dreamcube.mazegame.server.maze.server_bots.ServerSideClient
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class represents a client connection. It contains coroutines for reading commands from and sending messages to a single client.
 */
class ClientConnection(
    private val server: MazeServer,
    private val parentScope: CoroutineScope,
    private val socket: Socket
) : CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClientConnection::class.java)
    }

    /**
     * The channel for outgoing messages to the client.
     */
    private val outgoing = Channel<Message>(Channel.UNLIMITED)

    /**
     * The read job.
     */
    lateinit var readJob: Job

    /**
     * The write job.
     */
    lateinit var writeJob: Job

    /**
     * The player id.
     */
    val id: Int
        get() = player.id

    /**
     * The player nick.
     */
    val nick: String
        get() = player.nick

    /**
     * Indicates, if this connection should start as spectator.
     */
    val startAsSpectator: Boolean
        get() = player.nick.startsWith(server.serverConfiguration.serverBots.specialBots.spectator)

    var spectator: Boolean = false
        private set

    /**
     * The current status of the client connection.
     */
    var status: ConnectionStatus = ConnectionStatus.UNKNOWN
        private set

    /**
     * Indicator, if this connection was logged in at any point in time.
     */
    var wasEverLoggedIn: Boolean = false
        private set

    /**
     * This object handles the delay compensation for this client connection, if the feature is active. It also handles delay penalties, even if the
     * delay compensation feature is inactive.
     */
    val delayCompensator: DelayCompensator = DelayCompensator(this, server)

    /**
     * Spam protection for the chat function.
     */
    val chatControl: ClientChatControl = ClientChatControl()

    /**
     * This flag is set, whenever a ready-message is sent to the client.
     */
    val isReady = AtomicBoolean(false)

    /**
     * The player object associated with this client connection.
     */
    var player: Player = Player(-1, "")

    /**
     * The server-sided client that is associated with this client connection.
     */
    var serverSideClient: ServerSideClient? = null
        private set

    /**
     * Indicates, that this client connection is actually a server sided client connection.
     */
    val isServerSided: Boolean
        get() = serverSideClient != null

    /**
     * If true, this connection belongs to the auto trapeater
     */
    val isAutoTrapeater: Boolean
        get() = nick.startsWith(server.autoTrapeaterHandler.botAlias)

    /**
     * If true, this connection belongs to the frenzy bot
     */
    val isFrenzyBot: Boolean
        get() = nick.startsWith(server.frenzyHandler.botAlias)

    val performDelayCompensation: Boolean
        get() {
            val serverConfiguration = server.serverConfiguration
            val specialBots = serverConfiguration.serverBots.specialBots
            return serverConfiguration.game.delayCompensation && !(isServerSided && specialBots.isSpecial(nick))
        }

    suspend fun getDelayOffset(): Long = if (performDelayCompensation)
        delayCompensator.getTurnTimeOffset()
    else
        delayCompensator.penaltyTime

    /**
     * Starts this client connection. This includes the coroutine for sending messages and reading commands.
     */
    fun start() = launch(start = CoroutineStart.UNDISPATCHED) {
        writeJob = launch { writeLoop() }
        readJob = launch { readLoop() }
        status = ConnectionStatus.CONNECTED

        readJob.join()
        stop()
    }

    /**
     * Stops this client connection.
     */
    suspend fun stop() {
        if (status == ConnectionStatus.DEAD) {
            return
        }
        if (status == ConnectionStatus.PLAYING) {
            server.activePlayers.decrementAndGet()
        }
        status = ConnectionStatus.DYING
        server.removeClient(id)
        if (writeJob.isActive) {
            sendMessage(createQuitMessage())
            if (wasEverLoggedIn) {
                LOGGER.info("Client with id '$id' and nick '$nick' successfully logged out.")
            }
        }
        outgoing.close()
        writeJob.join()
        cancel()
        status = ConnectionStatus.DEAD
        LOGGER.debug("Connection closed: '{}'", socket.remoteAddress)
        socket.close()
    }

    /**
     * Main loop for reading commands from the client. Reads directly from the [socket]'s [ByteReadChannel].
     */
    private suspend fun readLoop() {
        try {
            val input: ByteReadChannel = socket.openReadChannel()
            while (isActive) {
                val line = input.readUTF8Line() ?: break
                LOGGER.debug("Received line: '{}'", line)
                val command = createCommand(this, server, line)
                server.commandExecutor.addCommand(command)
            }
        } catch (_: ClosedByteChannelException) {
            // do nothing
        }
    }

    /**
     * Main loop for sending messages to the client. Reads the messages form the [outgoing] channel and writes directly to the [socket]'s
     * [ByteWriteChannel].
     */
    private suspend fun writeLoop() {
        val instantFlush: Boolean = server.serverConfiguration.connection.instantFlush
        try {
            val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = false)
            for (message: Message in outgoing) {
                if (message.msg.isNotEmpty()) {
                    output.writeStringUtf8("${message.msg}\n")
                    LOGGER.debug("Sent message: '{}'", message.msg)
                }
                if (instantFlush || message.lastMessage) {
                    output.flush()
                }
            }
        } catch (_: ClosedByteChannelException) {
            // do nothing
        }
    }

    /**
     * Adds a new message to the [outgoing] channel.
     */
    suspend fun sendMessage(message: Message) {
        try {
            outgoing.send(message)
        } catch (_: ClosedSendChannelException) {
            LOGGER.error("Outgoing channel was already closed.")
        }
    }

    /**
     * Indicates the login status of the client.
     */
    fun loggedIn(): Boolean = status >= ConnectionStatus.LOGGED_IN && status <= ConnectionStatus.PLAYING

    /**
     * Sets the client connection to the login status if it is not already logged in.
     */
    suspend fun login(id: Int, nick: String) {
        if (status == ConnectionStatus.CONNECTED) {
            wasEverLoggedIn = true
            status = ConnectionStatus.LOGGED_IN
            this.player = Player(id, nick)
        } else {
            sendMessage(createErrorInfoMessage(InfoCode.ALREADY_LOGGED_IN))
        }
    }

    /**
     * Associates this client connection with a server-side client. It only works if this client connection is not already associated with a
     * server-side client and the ids match.
     */
    fun associateWithServerSideClient(serverSideClient: ServerSideClient) {
        if (this.serverSideClient == null && serverSideClient.clientId == id) {
            this.serverSideClient = serverSideClient
        }
    }

    /**
     * Sets the client connection into the status [ConnectionStatus.PLAYING] if it is logged in.
     */
    fun play() {
        if (status == ConnectionStatus.LOGGED_IN || status == ConnectionStatus.SPECTATING) {
            status = ConnectionStatus.PLAYING
            spectator = false
            player.resetScore() // sets score to zero (which is redundant here), and sets the start play timestamp to now (which is why we call it)
            server.activePlayers.incrementAndGet()
        }
    }

    /**
     * Sets the client connection into the status [ConnectionStatus.SPECTATING] if it is logged in.
     */
    fun spectate() {
        val previousStatus = status
        if (previousStatus == ConnectionStatus.LOGGED_IN || previousStatus == ConnectionStatus.PLAYING) {
            status = ConnectionStatus.SPECTATING
            spectator = true
        }
        if (previousStatus == ConnectionStatus.PLAYING) {
            server.activePlayers.decrementAndGet()
        }
    }

    /**
     * Indicates that this client connection is ready for the next client command and informs the client about it.
     */
    suspend fun ready() {
        isReady.set(true)
        if (status != ConnectionStatus.PLAYING) {
            return
        }
        if (performDelayCompensation) {
            delayCompensator.startTimer()
        }
        sendMessage(createReadyMessage())
    }

    /**
     * Is called, whenever the client wants moves. Contains the logic for waiting until the next call to ready(). If the player is not allowed to
     * move, false is returned.
     */
    suspend fun unready(): Boolean {
        isReady.set(false)
        if (performDelayCompensation) {
            delayCompensator.stopTimer()
        }
        val movementAllowed: Boolean = chatControl.onMove()
        if (server.frenzyHandler.active) {
            server.frenzyHandler.handle()
        }
        val delayTime: Long = server.gameSpeed.delay + getDelayOffset()
        launch {
            delay(delayTime)
            ready()
        }
        return movementAllowed
    }
}

