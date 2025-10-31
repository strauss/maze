package de.dreamcube.mazegame.server.maze

import de.dreamcube.mazegame.common.api.FreeNickMapping
import de.dreamcube.mazegame.common.api.GameSpeed
import de.dreamcube.mazegame.common.api.MazeServerConfigurationDto
import de.dreamcube.mazegame.common.api.NickMappingsDto
import de.dreamcube.mazegame.common.maze.*
import de.dreamcube.mazegame.server.contest.ContestConfiguration
import de.dreamcube.mazegame.server.contest.ContestController
import de.dreamcube.mazegame.server.maze.commands.control.GoCommand
import de.dreamcube.mazegame.server.maze.commands.control.OccupationResult
import de.dreamcube.mazegame.server.maze.game_events.GameEventControl
import de.dreamcube.mazegame.server.maze.generator.MazeGenerator
import de.dreamcube.mazegame.server.maze.generator.WallBasedMazeGenerator
import de.dreamcube.mazegame.server.maze.generator.generateMazeFromConfiguration
import de.dreamcube.mazegame.server.maze.server_bots.AutoTrapeaterHandler
import de.dreamcube.mazegame.server.maze.server_bots.ClientWrapper
import de.dreamcube.mazegame.server.maze.server_bots.FrenzyHandler
import de.dreamcube.mazegame.server.maze.server_bots.ServerSideClient
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.round

/**
 * Central maze server class. Contains most of the functionality of the basic game mechanics. The [serverConfiguration] contains the configuration
 * parameters for the server.
 */
class MazeServer(
    val serverConfiguration: MazeServerConfigurationDto,
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MazeServer::class.java)
        val serverMap: MutableMap<Int, MazeServer> = ConcurrentHashMap()
    }

    /**
     * The server port
     */
    private val port
        get() = serverConfiguration.connection.port

    /**
     * The current game speed. Initially it is set by the configuration but can be changed at runtime.
     */
    var gameSpeed: GameSpeed = serverConfiguration.game.initialSpeed
        internal set

    /**
     * The server socket.
     */
    private lateinit var serverSocket: ServerSocket

    /**
     * The last player id that has been generated. It is used for the id generation. It is always counted up.
     */
    private var lastId = AtomicInteger(0)

    /**
     * This structure contains all active client connections.
     */
    private val clientConnectionsById: MutableMap<Int, ClientConnection> = LinkedHashMap()

    /**
     * This number counts all active players (not all active client connections are also active players).
     */
    val activePlayers = AtomicInteger(0)

    /**
     * Mutex for limiting the access to [clientConnectionsById].
     */
    private val clientConnectionMutex = Mutex()

    /**
     * This structure contains all baits that are currently present in the maze.
     */
    internal val baitsById: MutableMap<Long, ServerBait> = LinkedHashMap()

    /**
     * Mutex for limiting the access to [baitsById].
     */
    internal val baitMutex = Mutex()

    /**
     * This class provides random positions for bait generation, but also player positioning when logging in for the first time or when they are
     * teleported to random locations.
     */
    internal val positionProvider: PositionProvider

    /**
     * This class is responsible for generating new baits.
     */
    internal val baitGenerator: BaitGenerator = BaitGenerator(this)

    /**
     * This class handles specific game events.
     */
    internal val gameEventControl: GameEventControl = GameEventControl(this, scope)

    /**
     * This is the bait count that is suitable for the current number of walkable fields.
     */
    val baseBaitCount: Int

    /**
     * This number is the "desired" bait count for the maze. Whenever baits are generated, they are filled up until they reach this number. If the
     * number is exceeded, no new baits are generated, until this number is reached again.
     */
    var desiredBaitCount: AtomicInteger

    /**
     * The maximum number of allowed traps in the maze.
     */
    val maxTrapCount: Int

    /**
     * The current bait count.
     */
    internal var currentBaitCount = AtomicInteger(0)

    /**
     * The current trap count.
     */
    internal var currentTrapCount = AtomicInteger(0)

    /**
     * The current visible trap count.
     */
    internal var visibleTrapCount = AtomicInteger(0)

    /**
     * This object is responsible for executing the commands that are received by all clients.
     */
    val commandExecutor = CommandExecutor(scope)

    /**
     * The maze (only walls and paths, no other objects).
     */
    val maze: Maze

    /**
     * Contains all bot names, that can be spawned on the server side.
     */
    internal val availableBotNames: Set<String>

    /**
     * Maps bot names to possible nicknames.
     */
    internal val botNamesToPossibleNickNameMap: Map<String, Set<String>>

    /**
     * Flag indicating if the auto trapeater is enabled.
     */
    internal var autoTrapeaterEnabled: Boolean

    /**
     * Flag indicating if game events are enabled.
     */
    internal var gameEventsEnabled: Boolean
        private set

    /**
     * Handles the auto trapeater logic.
     */
    internal val autoTrapeaterHandler: AutoTrapeaterHandler = AutoTrapeaterHandler(this)

    /**
     * Handles the frenzy bot logic.
     */
    internal val frenzyHandler: FrenzyHandler = FrenzyHandler(this)

    /**
     * Controller for a running contest.
     */
    internal var contestController: ContestController? = null

    init {
        val mzg: MazeGenerator =
            WallBasedMazeGenerator(serverConfiguration.maze.generatorParameters.templateFillStartPoints)
        maze = mzg.generateMazeFromConfiguration(serverConfiguration.maze)
        positionProvider = PositionProvider(maze)
        LOGGER.info("Maze dimension: ${maze.width} x ${maze.height}")
        val totalFields = maze.width * maze.height
        LOGGER.info("Maze total fields: $totalFields")
        val walkableFields = positionProvider.walkablePositionsSize
        LOGGER.info("Maze walkable fields: $walkableFields (${round((10000.0 * walkableFields) / totalFields) / 100.0} %)")
        baseBaitCount = walkableFields / serverConfiguration.game.baitGenerator.objectDivisor
        LOGGER.info("Base bait count: $baseBaitCount")
        maxTrapCount = max(1, baseBaitCount / serverConfiguration.game.baitGenerator.trapDivisor)
        LOGGER.info("Max trap count: $maxTrapCount")
        desiredBaitCount = AtomicInteger(if (serverConfiguration.game.generateBaitsAtStart) baseBaitCount else 0)
        availableBotNames = ClientWrapper.determineAvailableBotNames()
        val trapeaterName: String = serverConfiguration.serverBots.specialBots.trapeater
        autoTrapeaterEnabled = serverConfiguration.game.autoTrapeater && availableBotNames.contains(trapeaterName)
        if (autoTrapeaterEnabled) {
            LOGGER.info("Auto trapeater is enabled: '$trapeaterName'")
        }
        gameEventsEnabled = serverConfiguration.game.events.enabled
        botNamesToPossibleNickNameMap = buildMap {
            val nickNameMappings: NickMappingsDto = serverConfiguration.serverBots.nickMappings
            val specialBots = serverConfiguration.serverBots.specialBots
            put(specialBots.dummy, nickNameMappings.dummyNames + specialBots.dummy)
            put(specialBots.trapeater, nickNameMappings.trapeaterNames + specialBots.trapeater)
            put(specialBots.frenzy, nickNameMappings.frenzyNames + specialBots.frenzy)
            for (currentMapping: FreeNickMapping in nickNameMappings.freeNickMappings) {
                put(currentMapping.botName, currentMapping.nickNames)
            }
        }
    }

    /**
     * Starts the server. Contains the main loop for accepting new client connections.
     */
    fun start(): Deferred<Unit> {
        val result = CompletableDeferred<Unit>()
        scope.launch {
            commandExecutor.start()
            gameEventControl.start()
            try {
                val selector = SelectorManager(Dispatchers.IO)
                serverSocket = aSocket(selector).tcp().bind(port = port)
                serverMap[port] = this@MazeServer
                LOGGER.info("Maze server started, listening on $port")
                result.complete(Unit)
            } catch (ex: Throwable) {
                result.completeExceptionally(ex)
                cancel("Server start failed!", ex)
            }
            launch { initializeServerSideBots() }
            while (isActive) {
                val socket: Socket = serverSocket.accept()
                LOGGER.info("Client connection accepted: " + socket.remoteAddress)
                launch {
                    val clientConnection = ClientConnection(this@MazeServer, this, socket)
                    clientConnection.start()

                    // check if we have space left
                    val relevantClientCount = getRelevantClientCount()
                    if (relevantClientCount >= serverConfiguration.connection.maxClients) {
                        clientConnection.sendMessage(createErrorInfoMessage(InfoCode.TOO_MANY_CLIENTS))
                        clientConnection.stop()
                    } else {
                        // let them in
                        clientConnection.sendMessage(createServerVersionMessage())
                        // if they do not log in properly in the time limit, we throw them out again
                        delay(serverConfiguration.connection.loginTimeout)
                        if (!clientConnection.wasEverLoggedIn && !clientConnection.loggedIn()) {
                            clientConnection.sendMessage(createErrorInfoMessage(InfoCode.LOGIN_TIMEOUT))
                            clientConnection.stop()
                        }
                    }
                }
            }
        }
        return result
    }

    internal suspend fun getRelevantClientCount(): Int {
        var connectedClientCount: Int = clientConnectionMutex.withLock { clientConnectionsById.size }
        if (autoTrapeaterHandler.active) {
            connectedClientCount -= 1
        }
        if (frenzyHandler.active) {
            connectedClientCount -= 1
        }
        return connectedClientCount
    }

    /**
     * Shuts down the server ... hopefully in a graceful manner.
     */
    suspend fun stop(): Result<Unit> {
        LOGGER.info("Shutting down server listening on port '$port'.")
        return runCatching {
            clientConnectionMutex.withLock {
                clientConnectionsById.values.forEach { connection -> connection.stop() }
            }
            scope.cancel()
            serverMap.remove(port)
            serverSocket.close()
        }
    }

    /**
     * Checks if a nick is already taken.
     */
    suspend fun containsNick(nick: String): Boolean {
        clientConnectionMutex.withLock {
            clientConnectionsById.forEach { (_, connection) -> if (connection.nick == nick) return true }
        }
        return false
    }

    /**
     * Is used for clients to register themselves with the server. One could also call it "login".
     */
    suspend fun registerClient(connection: ClientConnection, nick: String) {
        // assign id and log player in
        val newId = lastId.incrementAndGet()
        connection.login(newId, nick)
        clientConnectionMutex.withLock {
            clientConnectionsById[newId] = connection
            connection.sendMessage(createWelcomeMessage(newId))
        }
        LOGGER.info("Client with id '${connection.id}' and nick '${connection.nick}' successfully logged in.")

        val newPlayer = connection.player
        if (connection.startAsSpectator) {
            // Spectators won't get no random position and other players won't learn about them.
            initPlayerPosition(newPlayer, Position.spectatorPosition)
            return
        }

        // assign a random position to player
        val newPlayersPosition: Position = positionProvider.randomFreePosition()
        initPlayerPosition(newPlayer, newPlayersPosition)

        // tell all other players about it
        if (!connection.isServerSided && newPlayer.nick != serverConfiguration.serverBots.specialBots.trapeater) {
            sendToAllPlayers(createServerInfoMessage("${newPlayer.nick} joined the game.").thereIsMore())
        }
        sendToAllPlayers(
            createJoinMessage(newPlayer).thereIsMore(),
            createPlayerPositionAppearMessage(newPlayer).thereIsMore(),
            createPlayerScoreChangedMessage(newPlayer)
        )
    }

    /**
     * Removes a client from the server.
     */
    internal fun removeClient(id: Int) {
        scope.launch {
            val messagesForAll: Array<Message> = buildList {
                val removed: ClientConnection? = clientConnectionMutex.withLock { clientConnectionsById.remove(id) }
                if (removed != null && !removed.spectator) {
                    val player: Player = removed.player
                    releasePlayerPosition(player)
                    add(createPlayerPositionVanishMessage(player).thereIsMore())
                    add(createLeaveMessage(id).thereIsMore())
                    if (!removed.isServerSided && player.nick != serverConfiguration.serverBots.specialBots.trapeater) {
                        add(createServerInfoMessage("${player.nick} left the game."))
                    } else {
                        add(createEmptyLastMessage())
                    }
                }
            }.toTypedArray()
            if (messagesForAll.isNotEmpty()) {
                sendToAllPlayers(*messagesForAll)
            }
        }
    }

    /**
     * Retrieves the client connection with the given [id], if it exists.
     */
    internal suspend fun getClientConnection(id: Int?): ClientConnection? =
        clientConnectionMutex.withLock { clientConnectionsById[id] }

    /**
     * Spawns a [player] at the given [position].
     */
    internal suspend fun initPlayerPosition(player: Player, position: Position) {
        player.x = position.x
        player.y = position.y
        if (position != Position.spectatorPosition) {
            maze.occupyPosition(player.x, player.y)
        }
    }

    /**
     * Removes a [player] from their position. It is mainly used, when the player disconnects from the game.
     */
    internal suspend fun releasePlayerPosition(player: Player) {
        maze.releasePosition(player.x, player.y)
    }

    /**
     * Moves or turns a [player].
     */
    suspend fun changePlayerPosition(player: Player, newPosition: Position, viewDirection: ViewDirection) {
        changePlayerPosition(player, newPosition.x, newPosition.y, viewDirection)
    }

    /**
     * Moves or turns a [player].
     */
    suspend fun changePlayerPosition(player: Player, x: Int, y: Int, viewDirection: ViewDirection) {
        maze.move(player.x, player.y, x, y)
        player.x = x
        player.y = y
        player.viewDirection = viewDirection
    }

    /**
     * Teleports a [player] to a random position.
     */
    suspend fun teleportPlayerRandomly(player: Player, causingId: Int? = null): Message {
        val newPosition: Position = positionProvider.randomPositionForTeleport()
        val newDirection: ViewDirection = ViewDirection.random()
        changePlayerPosition(player, newPosition, newDirection)
        return if (causingId == null) {
            createPlayerTeleportByTrapMessage(player)
        } else {
            createPlayerTeleportByCollisionMessage(player, causingId)
        }
    }

    /**
     * Teleports the given [player] to the position given by [newX] and [newY], if this is possible.
     */
    suspend fun teleportPlayer(player: Player, newX: Int, newY: Int): Pair<OccupationResult, Message?> =
        withUnoccupiedPosition(newX, newY) {
            changePlayerPosition(player, newX, newY, player.viewDirection)
            OccupationResult.SUCCESS to createPlayerTeleportMessage(player)
        }

    suspend fun putBait(
        type: BaitType,
        x: Int,
        y: Int,
        visible: Boolean,
        reappearOffset: Long
    ): Pair<OccupationResult, Message?> =
        withUnoccupiedPosition(x, y) {
            val newBait = ServerBait(type, x, y)
            if (!visible) {
                newBait.internalMakeInvisible(reappearOffset)
            }
            val message: Message? = addBait(newBait)
            OccupationResult.SUCCESS to message
        }

    private suspend fun withUnoccupiedPosition(
        x: Int,
        y: Int,
        onSuccess: suspend () -> Pair<OccupationResult, Message?>
    ): Pair<OccupationResult, Message?> {
        return when {
            x !in 0..<maze.width || y !in 0..<maze.height -> OccupationResult.FAIL_OUT_OF_BOUNDS to null
            maze[x, y] != Maze.PATH -> OccupationResult.FAIL_NO_PATH to null
            maze.isOccupied(x, y) -> OccupationResult.FAIL_OCCUPIED to null
            else -> onSuccess()
        }
    }

    /**
     * Delivers the map data to the given [connection].
     */
    suspend fun sendMapData(connection: ClientConnection) {
        connection.sendMessage(createMazeHeaderMessage(maze.width, maze.height).thereIsMore())
        maze.toString().lines().forEach { connection.sendMessage(it.asMessage().thereIsMore()) }
        connection.sendMessage(createEmptyLastMessage())
    }

    /**
     * Delivers the information about all players to the given [targetConnection].
     */
    suspend fun sendAllPlayerInfos(targetConnection: ClientConnection) {
        val messages: List<Message> = buildList(serverConfiguration.connection.maxClients * 3) {
            clientConnectionMutex.withLock {
                for (currentConnection: ClientConnection in clientConnectionsById.values) {
                    if (currentConnection.startAsSpectator ||
                        (currentConnection != targetConnection && currentConnection.status != ConnectionStatus.PLAYING)
                    ) {
                        continue
                    }
                    val currentPlayer: Player = currentConnection.player
                    add(createJoinMessage(currentPlayer).thereIsMore())
                    add(createPlayerPositionAppearMessage(currentPlayer).thereIsMore())
                    add(createPlayerScoreChangedMessage(currentPlayer).thereIsMore())
                }
            }
            add(createEmptyLastMessage())
        }
        messages.forEach { targetConnection.sendMessage(it) }
    }

    /**
     * Delivers the information about all baits to the given [targetConnection].
     */
    suspend fun sendAllBaitInfos(targetConnection: ClientConnection) {
        val messages: MutableList<Message> = ArrayList()
        baitMutex.withLock {
            for (currentBait: ServerBait in baitsById.values) {
                if (currentBait.visibleToClients) {
                    messages.add(createBaitGeneratedMessage(currentBait).thereIsMore())
                }
            }
        }
        messages.add(createEmptyLastMessage())
        messages.forEach { targetConnection.sendMessage(it) }
    }

    /**
     * Delivers the given [messages] to all playing players and spectators.
     */
    suspend fun sendToAllPlayers(vararg messages: Message, except: Int = -1) {
        val playingPlayerConnections = getAllPlayingPlayerConnections()
        for (connection in playingPlayerConnections) {
            if (except == connection.id) {
                continue
            }
            for (message in messages) {
                connection.sendMessage(message)
            }
        }
    }

    /**
     * Determines all active players and spectators.
     */
    internal suspend fun getAllPlayingPlayerConnections(): List<ClientConnection> {
        val playingPlayerConnections: List<ClientConnection> = buildList {
            clientConnectionMutex.withLock {
                for (currentConnection: ClientConnection in clientConnectionsById.values) {
                    if (currentConnection.status == ConnectionStatus.PLAYING || currentConnection.status == ConnectionStatus.SPECTATING) {
                        add(currentConnection)
                    }
                }
            }
        }
        return playingPlayerConnections
    }

    /**
     * Resets all scores and generates the notification messages.
     */
    suspend fun resetAllScores(): List<Message> {
        val messages: List<Message> = buildList {
            getAllPlayingPlayerConnections().forEach { currentConnection ->
                currentConnection.player.resetScore()
                add(createPlayerScoreChangedMessage(currentConnection.player).thereIsMore())
            }
            if (isNotEmpty()) {
                add(createEmptyLastMessage())
            }
        }
        return messages
    }

    /**
     * Fills baits up to [desiredBaitCount]. If the number of baits exceeds this value, no new baits are generated.
     */
    suspend fun replaceBaits(): List<Message> {
        val result: List<Message> = buildList {
            // check for reappear
            val baitsToReappear: List<ServerBait> = baitGenerator.findInvisibleBaitsThatShouldReappear()
            for (bait: ServerBait in baitsToReappear) {
                if (bait.type == BaitType.TRAP) {
                    visibleTrapCount.incrementAndGet()
                }
                add(createBaitGeneratedMessage(bait).thereIsMore())
            }
            // create new bait(s) (or none, if we already have enough of them)
            internalFillBaits()
        }
        if (autoTrapeaterEnabled) {
            autoTrapeaterHandler.handle()
        }
        return result
    }

    /**
     * Adds the given [newBait] to the game. The creation message is only generated, if the bait is visible.
     */
    private suspend fun addBait(newBait: ServerBait): Message? {
        baitMutex.withLock {
            baitsById[newBait.id] = newBait
        }
        maze.occupyPosition(newBait.x, newBait.y)
        currentBaitCount.incrementAndGet()
        if (newBait.type == BaitType.TRAP) {
            currentTrapCount.incrementAndGet()
            if (newBait.visibleToClients) {
                visibleTrapCount.incrementAndGet()
            }
        }
        return if (newBait.visibleToClients) createBaitGeneratedMessage(newBait) else null
    }

    /**
     * Removes the given bait from the server. The remove message is only generated, if the bait is visible.
     */
    suspend fun removeBait(bait: ServerBait): Message? {
        val removedBait = baitsById.remove(bait.id)
        assert(removedBait == bait)
        currentBaitCount.decrementAndGet()
        if (bait.type == BaitType.TRAP) {
            currentTrapCount.decrementAndGet()
            if (bait.visibleToClients) {
                visibleTrapCount.decrementAndGet()
            }
        }
        maze.releasePosition(bait.x, bait.y)
        return if (bait.visibleToClients) createBaitCollectedMessage(bait) else null
    }

    internal fun changeBait(bait: ServerBait, newType: BaitType): List<Message> {
        return buildList {
            if (bait.visibleToClients) {
                add(createBaitCollectedMessage(bait).thereIsMore())
            } else {
                bait.makeVisible()
                if (bait.type == BaitType.TRAP) {
                    visibleTrapCount.incrementAndGet()
                }
            }
            if (bait.type == BaitType.TRAP) {
                visibleTrapCount.decrementAndGet()
                currentTrapCount.decrementAndGet()
            }
            bait.type = newType
            if (newType == BaitType.TRAP) {
                visibleTrapCount.incrementAndGet()
                currentTrapCount.incrementAndGet()
            }
            add(createBaitGeneratedMessage(bait).thereIsMore())
        }
    }

    /**
     * Initializes the maze with baits.
     */
    internal suspend fun fillBaits(): List<Message> {
        LOGGER.info("Generating baits...")
        val messages: List<Message> = buildList {
            internalFillBaits()
            if (isNotEmpty()) {
                add(createEmptyLastMessage())
            }
        }
        val invisibleBaits =
            baitMutex.withLock { baitsById.values.asSequence().filter { !it.visibleToClients } }.count()
        LOGGER.info("Generated ${currentBaitCount.get()} baits ($invisibleBaits are invisible).")
        return messages
    }

    /**
     * Fills the baits until [desiredBaitCount] is reached.
     */
    private suspend fun MutableList<Message>.internalFillBaits() {
        while (currentBaitCount.get() < desiredBaitCount.get()) {
            val newBait: ServerBait = baitGenerator.createRandomBait()
            val message: Message? = addBait(newBait)
            if (message != null) {
                add(message.thereIsMore())
            }
        }
    }

    /**
     * Withdraws all baits matching the given [filter] on their type. If no [filter] is given, all baits are removed.
     */
    suspend fun withdrawBaits(filter: (BaitType) -> Boolean = { true }): List<Message> {
        val baitsToWithdraw: List<ServerBait> = buildList {
            baitMutex.withLock {
                baitsById.values.forEach { bait ->
                    if (filter.invoke(bait.type)) {
                        add(bait)
                    }
                }
            }
        }
        return buildList {
            baitsToWithdraw.forEach { bait ->
                val message: Message? = removeBait(bait)
                if (message != null) {
                    add(message.thereIsMore())
                }
            }
            if (isNotEmpty()) {
                add(createEmptyLastMessage())
            }
        }
    }

    /**
     * Searches for the bait at the given position ([x], [y]), if such a bait exists.
     */
    suspend fun getBaitAt(x: Int, y: Int): ServerBait? {
        baitMutex.withLock {
            for (currentBait: ServerBait in baitsById.values) {
                if (currentBait.x == x && currentBait.y == y) {
                    return currentBait
                }
            }
        }
        return null
    }

    /**
     * Searches for the player at the given position ([x], [y]), if such a player exists.
     */
    suspend fun getPlayerAt(x: Int, y: Int): Player? {
        clientConnectionMutex.withLock {
            for (currentClientConnection: ClientConnection in clientConnectionsById.values) {
                val currentPlayer = currentClientConnection.player
                if (currentPlayer.x == x && currentPlayer.y == y) {
                    return currentPlayer
                }
            }
        }
        return null
    }

    /**
     * Initializes all configured server-side bots.
     */
    suspend fun initializeServerSideBots() {
        val launchAliases: List<String> = serverConfiguration.serverBots.autoLaunch
        if (launchAliases.isNotEmpty()) {
            for (currentAlias in launchAliases) {
                spawnServerSideBot(currentAlias)
            }
        }
        if (desiredBaitCount.get() > 0) {
            commandExecutor.addCommand(GoCommand(this@MazeServer))
        }
    }

    /**
     * Spawns a server-side bot and associates it with a client connection. The call waits until the association is complete. If a trapeater is tried
     * spawn, the auto trapeater option will be activated instead. The frenzy bot can only be spawned if it is not running already.
     */
    suspend fun spawnServerSideBot(currentAlias: String): ClientConnection? {
        if (currentAlias == serverConfiguration.serverBots.specialBots.trapeater) {
            if (autoTrapeaterEnabled) {
                LOGGER.warn("Auto trapeater option is already active. The auto trapeater will be spawned automatically.")
            } else {
                LOGGER.info("Auto trapeater option will be activated. The bot will spawn automatically.")
                autoTrapeaterEnabled = true
            }
            return null
        } else if (currentAlias == serverConfiguration.serverBots.specialBots.frenzy) {
            frenzyHandler.spawnManually(false)
            return getClientConnection(frenzyHandler.client?.clientId)
        }
        val serverSideClient: ServerSideClient? = internalSpawnServerSideBot(currentAlias)
        return associateBotWithClientConnection(serverSideClient)
    }

    /**
     * Spawns a server-side bot.
     */
    internal fun internalSpawnServerSideBot(alias: String): ServerSideClient? {
        if (availableBotNames.contains(alias)) {
            val possibleNickNames: Set<String> = botNamesToPossibleNickNameMap[alias] ?: setOf(alias)
            return ClientWrapper.createServerSideClient(alias, port, possibleNickNames.random())
        }
        return null
    }

    /**
     * Associates a server-side client with its [ClientConnection]. It does so in a new coroutine.
     */
    fun associateBotWithClientConnectionInTheBackground(serverSideClient: ServerSideClient?) {
        scope.launch { associateBotWithClientConnection(serverSideClient) }
    }

    /**
     * Associates a bot with its [ClientConnection]. It can take some time to wait for the id.
     */
    internal suspend fun associateBotWithClientConnection(serverSideClient: ServerSideClient?): ClientConnection? {
        if (serverSideClient != null) {
            do {
                delay(gameSpeed.delay) // unlimited could be a problem...
                if (serverSideClient.connectionFailed) {
                    LOGGER.error("Server side client could not connect.")
                    return null
                }
                val id: Int = serverSideClient.clientId
                val clientConnection: ClientConnection? = getClientConnection(id)
                clientConnection?.associateWithServerSideClient(serverSideClient)
                if (clientConnection != null) {
                    return clientConnection
                }
            } while (serverSideClient.clientId <= 0)
        }
        return null
    }

    internal fun contestRunning(): Boolean = contestController?.contestRunning ?: false

    internal suspend fun stopContest() = contestController?.stop()

    internal fun startContest(configuration: ContestConfiguration): Boolean {
        if (contestRunning()) {
            return false
        }
        contestController = ContestController(this, scope, configuration)
        contestController?.start()
        return true
    }
}