package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto
import de.dreamcube.mazegame.client.maze.commands.ServerCommandParser
import de.dreamcube.mazegame.common.maze.CommandExecutor
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.common.maze.PROTOCOL_VERSION
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.io.IOException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Central maze client class.
 */
class MazeClient(
    val clientConfiguration: MazeClientConfigurationDto,
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val NO_ID: Int = -1
        private val LOGGER: Logger = LoggerFactory.getLogger(MazeClient::class.java)
    }

    private val serverAddress
        get() = clientConfiguration.serverAddress

    private val serverPort
        get() = clientConfiguration.serverPort

    /**
     * The socket for the connection.
     */
    private lateinit var socket: Socket

    /**
     * The current status of the connection.
     */
    var status: ConnectionStatus = ConnectionStatus.UNKNOWN

    /**
     * The id from the server.
     */
    var id = NO_ID
        private set

    /**
     * Channel for outgoing messages.
     */
    val outgoingMessages = Channel<Message>(Channel.UNLIMITED)

    /**
     * The client command executor.
     */
    val commandExecutor = CommandExecutor(scope)

    /**
     * The command Parser.
     */
    val commandParser = ServerCommandParser(scope, this@MazeClient, commandExecutor)

    lateinit var readJob: Job
    lateinit var writeJob: Job

    private lateinit var maze: Maze

    val selector = SelectorManager(Dispatchers.IO)

    fun start(): Deferred<Unit> {
        val result = CompletableDeferred<Unit>()
        scope.launch {
            try {
                socket = aSocket(selector).tcp().connect(clientConfiguration.serverAddress, clientConfiguration.serverPort)
            } catch (ex: IOException) {
                LOGGER.error("Connection error: ${ex.message}", ex)
                result.completeExceptionally(ex)
                return@launch
            }
            writeJob = launch { writeLoop() }
            commandExecutor.start()
            commandParser.start()
            readJob = launch { readLoop(scope) }
            status = ConnectionStatus.CONNECTED

            readJob.join()
            stop()
            result.complete(Unit)
        }
        return result
    }

    private suspend fun stop() {
        if (status == ConnectionStatus.DEAD) {
            return
        }
        try {
            status = ConnectionStatus.DYING
            // TODO: perform protocol specific stuff
            outgoingMessages.close()
            writeJob.join()
            scope.cancel()
        } finally {
            status = ConnectionStatus.DEAD
            LOGGER.debug("Connection closed: '{}'", socket.remoteAddress)
            socket.close()
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

    suspend fun sendMessage(message: Message) {
        try {
            outgoingMessages.send(message)
        } catch (_: ClosedSendChannelException) {
            LOGGER.error("Outgoing channel was already closed.")
        }
    }

    suspend fun connect(serverProtocolVersion: Int) {
        if (serverProtocolVersion == PROTOCOL_VERSION) {
            sendMessage(createHelloMessage(clientConfiguration.displayName))
        }
    }

    suspend fun loggedIn(id: Int) {
        if (status == ConnectionStatus.CONNECTED && id > 0) {
            this.id = id
            status = ConnectionStatus.LOGGED_IN
            sendMessage(createRequestMazeMessage())
        }
    }

    fun initializeMaze(width: Int, height: Int, mazeLines: List<String>) {
        if (status == ConnectionStatus.LOGGED_IN) {
            maze = Maze(width, height, mazeLines)
            status = ConnectionStatus.SPECTATING
        }
    }

    enum class ConnectionStatus() {
        UNKNOWN, CONNECTED, LOGGED_IN, SPECTATING, PLAYING, DYING, DEAD
    }
}