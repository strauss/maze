package de.dreamcube.mazegame.server.control

import de.dreamcube.mazegame.common.api.*
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.Player
import de.dreamcube.mazegame.server.contest.ContestConfiguration
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.control.*
import de.dreamcube.mazegame.server.maze.commands.game.BaitRushCommand
import de.dreamcube.mazegame.server.maze.commands.game.TransformBaitsCommand
import de.dreamcube.mazegame.server.maze.createSpeedChangeInfoMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds

/**
 * This class implements all control commands.
 */
object ServerController {

    private val LOGGER = LoggerFactory.getLogger(ServerController::class.java)

    /**
     * Launches a [MazeServer] with the given configuration
     */
    fun launchServer(serverConfiguration: MazeServerConfigurationDto): Deferred<Unit> {
        val maze = MazeServer(serverConfiguration)
        return maze.start()
    }

    /**
     * Stops all running [MazeServer]s.
     */
    fun quitAllMazeServers() {
        runBlocking {
            MazeServer.serverMap.values.forEach {
                val result = it.stop()
                if (result.isFailure) {
                    LOGGER.error(
                        "Failed to stop server at port '${it.serverConfiguration.connection.port}': ",
                        result.exceptionOrNull()
                    )
                }
            }
        }
    }

    /**
     * Retrieves the maze server with the given [serverId]. If none is found, null is returned and a [HttpStatusCode.NotFound] is responded.
     */
    private suspend fun getMazeServer(call: ApplicationCall, serverId: Int): MazeServer? {
        val mazeServer: MazeServer? = MazeServer.serverMap[serverId]
        if (mazeServer == null) {
            call.respond(HttpStatusCode.NotFound, "Server $serverId not found.")
        }
        return mazeServer
    }

    /**
     * Retrieves a [ClientConnection] with matching [playerId] from the given [server]. If none is found, null is returned and a
     * [HttpStatusCode.NotFound] is responded.
     */
    private suspend fun getClientConnection(
        call: ApplicationCall,
        server: MazeServer,
        playerId: Int
    ): ClientConnection? {
        val clientConnection: ClientConnection? = server.getClientConnection(playerId)
        if (clientConnection == null) {
            call.respond(HttpStatusCode.NotFound, "Player $playerId not found.")
        }
        return clientConnection
    }

    /**
     * Triggers bait generation if there are no baits.
     */
    suspend fun go(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        @Suppress("kotlin:S6518") // yeah ...
        if (server.desiredBaitCount.get() > 0) {
            call.respond(HttpStatusCode.PreconditionFailed, "Server $serverId already going.")
            return
        }
        server.commandExecutor.addCommand(GoCommand(server))
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Stops bait generation. If [now] stays unset, all traps are removed, but the remaining baits will stay until they've all been collected. If
     * [now] is set, all baits are removed immediately, which resembles the classic stop command in the legacy server.
     */
    suspend fun stop(call: ApplicationCall, serverId: Int, now: Boolean = false) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        @Suppress("kotlin:S6518") // yeah ...
        if (server.desiredBaitCount.get() <= 0) {
            call.respond(HttpStatusCode.PreconditionFailed, "Server $serverId already stopped.")
            return
        }
        server.commandExecutor.addCommand(StopCommand(server, now))
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Quits a single running [MazeServer].
     */
    suspend fun quitSingleMazeServer(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        server.stop()
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Launches a new maze server with the given configuration.
     */
    suspend fun launchNewMazeServer(call: ApplicationCall, serverConfiguration: MazeServerConfigurationDto) {
        val result: Deferred<Unit> = launchServer(serverConfiguration)
        val serverPort: Int = serverConfiguration.connection.port
        try {
            result.await()
            val mazeServer: MazeServer? = MazeServer.serverMap[serverPort]
            if (mazeServer != null) {
                call.respond(mazeServer.serverConfiguration)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    "The server creation did not yield an error, but the server was not found for an unknown reason."
                )
            }
        } catch (ex: Throwable) {
            LOGGER.error("Failed to start server on port '$serverPort': ", ex)
            call.respond(HttpStatusCode.InternalServerError, ex.message ?: "Could not launch server.")
        }
    }

    /**
     * Clears all scores of all players.
     */
    suspend fun clear(call: ApplicationCall, serverId: Int, stop: Boolean = true) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        if (stop) {
            server.commandExecutor.addCommand(StopCommand(server, true))
        }
        server.commandExecutor.addCommand(ClearCommand(server))
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Changes the game speed.
     */
    suspend fun changeSpeed(call: ApplicationCall, serverId: Int, speedAsString: String) {
        val newSpeed: GameSpeed? = GameSpeed.fromShortName(speedAsString)
        if (newSpeed == null) {
            call.respond(
                HttpStatusCode.NotFound,
                "Could not find speed for $speedAsString. Available speeds are ${GameSpeed.entries.map { it.shortName }}."
            )
            return
        }
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        server.gameSpeed = newSpeed
        server.sendToAllPlayers(createSpeedChangeInfoMessage(newSpeed))
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Transforms all baits into the given type and makes them visible. If the given type is [BaitType.TRAP], the auto trapeater will be spawned. If
     * this function is deactivated, no transformation happens.
     */
    suspend fun transformBaits(call: ApplicationCall, serverId: Int, type: BaitType) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        server.commandExecutor.addCommand(TransformBaitsCommand(server, type))
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Causes a temporary bait rush.
     */
    suspend fun baitRush(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        server.commandExecutor.addCommand(BaitRushCommand(server))
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Tries to execute the given [teleportCommand].
     */
    suspend fun teleportPlayer(call: ApplicationCall, serverId: Int, teleportCommand: TeleportCommandDto) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val clientConnection: ClientConnection = getClientConnection(call, server, teleportCommand.id) ?: return
        val command = TeleportCommand(server, clientConnection, teleportCommand.x, teleportCommand.y)
        server.commandExecutor.addCommand(command)
        val result: OccupationResult = command.reply.await()
        communicateOccupationResult(call, result)
    }

    /**
     * Tries to execute the given [putBaitCommand].
     */
    suspend fun putBait(call: ApplicationCall, serverId: Int, putBaitCommand: PutBaitCommandDto) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val baitType: BaitType = putBaitCommand.baitType
        val command = PutBaitCommand(
            server,
            baitType,
            putBaitCommand.x,
            putBaitCommand.y,
            putBaitCommand.visible,
            putBaitCommand.reappearOffset
        )
        server.commandExecutor.addCommand(command)
        val result: OccupationResult = command.reply.await()
        communicateOccupationResult(call, result)
    }

    private suspend fun communicateOccupationResult(call: ApplicationCall, result: OccupationResult) {
        when (result) {
            OccupationResult.SUCCESS -> call.respond(HttpStatusCode.NoContent)
            OccupationResult.FAIL_OUT_OF_BOUNDS -> call.respond(
                HttpStatusCode.BadRequest,
                "Coordinates are out of bounds."
            )

            OccupationResult.FAIL_NO_PATH -> call.respond(HttpStatusCode.BadRequest, "Position is not a walkable path.")
            OccupationResult.FAIL_OCCUPIED -> call.respond(HttpStatusCode.BadRequest, "Position is already occupied.")
        }
    }

    /**
     * Gets all active server ids.
     */
    suspend fun getReducedServerInformation(call: ApplicationCall) {
        val ids: List<Int> = MazeServer.serverMap.keys.sorted()
        val info: List<ReducedServerInformationDto> = ids.map { id ->
            val server: MazeServer = MazeServer.serverMap[id]!!
            val configuration = server.serverConfiguration
            val compactMaze: String = Base64.encode(server.maze.toCompactMaze().export())
            val spectator: String? =
                if (configuration.game.allowSpectator) configuration.serverBots.specialBots.spectator else null
            ReducedServerInformationDto(
                id,
                configuration.connection.maxClients,
                server.getRelevantClientCount(),
                server.gameSpeed.delay.toInt(),
                server.maze.width,
                server.maze.height,
                compactMaze,
                spectator
            )
        }
        call.respond(info)
    }

    /**
     * Retrieves the current server runtime information.
     */
    suspend fun getServerInformation(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val serverConfiguration: MazeServerConfigurationDto = server.serverConfiguration
        val connection: ConnectionDto = serverConfiguration.connection
        val maze: MazeGeneratorConfigurationDto = serverConfiguration.maze
        val mazeInformation =
            MazeInformationDto(
                maze.generatorMode.shortName,
                maze.generatorParameters,
                server.positionProvider.walkablePositionsSize
            )
        val game: GameDto = serverConfiguration.game
        val baitInformation = BaitInformationDto(
            game.baitGenerator,
            server.baseBaitCount,
            server.desiredBaitCount.get(),
            server.currentBaitCount.get(),
            server.maxTrapCount,
            server.currentTrapCount.get(),
            server.visibleTrapCount.get()
        )
        val gameInformation = GameInformationDto(
            server.gameSpeed.shortName,
            server.autoTrapeaterEnabled,
            game.allowSpectator,
            game.delayCompensation,
            baitInformation,
            server.activePlayers.get(),
            server.availableBotNames.toList()
        )

        call.respond(ServerInformationDto(connection, mazeInformation, gameInformation))
    }

    /**
     * Retrieves the server configuration object and sends a copy to the caller.
     */
    suspend fun getServerConfiguration(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val serverConfiguration: MazeServerConfigurationDto = server.serverConfiguration
        call.respond(serverConfiguration)
    }

    /**
     * Retrieves the information for one single player with the given [playerId].
     */
    suspend fun getPlayerInformation(call: ApplicationCall, serverId: Int, playerId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val clientConnection: ClientConnection = getClientConnection(call, server, playerId) ?: return
        val playerInformation: PlayerInformationDto = extractPlayerInformation(clientConnection)
        call.respond(playerInformation)
    }

    /**
     * Extracts the information about a sigle player.
     */
    private suspend fun extractPlayerInformation(connection: ClientConnection): PlayerInformationDto {
        val player: Player = connection.player
        val totalPlayTimeMs: Long = player.totalPlayTime
        val totalPlayTime = PlayTimeDto(totalPlayTimeMs, totalPlayTimeMs.milliseconds.toString())
        val currentPlayTimeMs: Long = player.currentPlayTime
        val currentPlayTime = PlayTimeDto(currentPlayTimeMs, currentPlayTimeMs.milliseconds.toString())
        val playerInformation = PlayerInformationDto(
            player.id,
            player.nick,
            player.score,
            connection.isServerSided,
            connection.getDelayOffset(),
            totalPlayTime,
            currentPlayTime,
            player.pointsPerMinute,
            player.moveTime,
            connection.spectator
        )
        return playerInformation
    }

    /**
     * Retrieves the information for all players.
     */
    suspend fun getPlayerInformation(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val clientConnections: List<ClientConnection> = server.getAllPlayingPlayerConnections()
        val resultList: List<PlayerInformationDto> = buildList {
            clientConnections.forEach { connection ->
                val playerInformation: PlayerInformationDto = extractPlayerInformation(connection)
                add(playerInformation)
            }
        }
        call.respond(resultList)
    }

    /**
     * Spawns a new bot with the given [nick], if it is available.
     */
    suspend fun spawnBot(call: ApplicationCall, serverId: Int, nick: String) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        if (!server.availableBotNames.contains(nick)) {
            call.respond(HttpStatusCode.NotFound, "Server side bot with nick '$nick' not found.")
            return
        }
        if (nick != server.serverConfiguration.serverBots.specialBots.trapeater &&
            nick != server.serverConfiguration.serverBots.specialBots.frenzy &&
            server.getRelevantClientCount() >= server.serverConfiguration.connection.maxClients
        ) {
            call.response.headers.append(HttpHeaders.RetryAfter, "69")
            call.respond(HttpStatusCode.ServiceUnavailable, "No more active connections allowed.")
            return
        }
        val command = SpawnCommand(server, nick)
        server.commandExecutor.addCommand(command)
        val clientConnection: ClientConnection? = command.reply.await()
        if (clientConnection == null) {
            if (nick == server.serverConfiguration.serverBots.specialBots.trapeater) {
                call.respond(
                    HttpStatusCode.Accepted,
                    "Auto trapeater will be activated or remain activated but not spawned directly."
                )
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Unknown error while spawning server side bot '$nick'."
                )
            }
            return
        }
        val playerInformation: PlayerInformationDto = extractPlayerInformation(clientConnection)
        call.respond(HttpStatusCode.Created, playerInformation)
    }

    /**
     * Kills a bot ... forcefully disconnects a client from the server.
     */
    suspend fun kill(call: ApplicationCall, serverId: Int, playerId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val clientConnection: ClientConnection = getClientConnection(call, server, playerId) ?: return
        server.commandExecutor.addCommand(KillCommand(server, clientConnection))
        call.respond(HttpStatusCode.NoContent)
    }

    /**
     * Retrieves contest information if there is any.
     */
    suspend fun getContestInformation(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        val contestConfiguration: ContestConfiguration? = server.contestController?.configuration
        if (contestConfiguration == null) {
            return call.respond(HttpStatusCode.NotFound, "No contest found.")
        }
        call.respond(HttpStatusCode.OK, contestConfiguration)
    }

    /**
     * Starts a new contest if no contest is already running.
     */
    suspend fun startContest(call: ApplicationCall, serverId: Int, contestConfiguration: ContestConfiguration) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        if (server.contestRunning()) {
            return call.respond(
                HttpStatusCode.PreconditionFailed,
                "Contest is already running. You have to wait until it is over or stop it manually."
            )
        }
        val success = server.startContest(contestConfiguration)
        if (success) {
            call.respond(HttpStatusCode.Accepted)
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Contest failed to start.")
        }
    }

    /**
     * Stops a running contest.
     */
    suspend fun stopContest(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        if (!server.contestRunning()) {
            return call.respond(HttpStatusCode.PreconditionFailed, "Contest is not started (yet).")
        }
        server.stopContest()
        server.contestController?.stop()
        call.respond(HttpStatusCode.Accepted)
    }

    /**
     * Triggers a contest report for connected clients.
     */
    suspend fun triggerReport(call: ApplicationCall, serverId: Int) {
        val server: MazeServer = getMazeServer(call, serverId) ?: return
        if (!server.contestRunning()) {
            return call.respond(HttpStatusCode.PreconditionFailed, "Contest is not running.")
        }
        server.contestController?.intermediateReport()
        call.respond(HttpStatusCode.Accepted)
    }

}