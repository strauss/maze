package de.dreamcube.mazegame.server.contest

import de.dreamcube.mazegame.server.maze.ClientConnectionStatus
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.ServerPlayer
import de.dreamcube.mazegame.server.maze.commands.control.ClearCommand
import de.dreamcube.mazegame.server.maze.commands.control.GoCommand
import de.dreamcube.mazegame.server.maze.commands.control.StopCommand
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class ContestController(
    private val server: MazeServer,
    private val parentScope: CoroutineScope,
    internal val configuration: ContestConfiguration
) : CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private const val CONTEST_OVER_OR_NOT_STARTED_MESSAGE = "Contest is already over or not started yet."
        private val LOGGER = LoggerFactory.getLogger(ContestController::class.java)
    }

    private val events = Channel<ContestEvent>(Channel.UNLIMITED)

    private var contestStartedAt: Long = 0L

    var everStarted: Boolean = false
        private set

    val contestRunning: Boolean
        get() = isActive && System.currentTimeMillis() < contestStartedAt + configuration.durationInMinutes.toMilliseconds()

    lateinit var eventJob: Job

    fun start() = launch(start = CoroutineStart.UNDISPATCHED) {
        initEvents()
        eventJob = processEvents()
    }

    suspend fun stop() {
        val now = System.currentTimeMillis()
        val duration: String = (now - contestStartedAt).milliseconds.toString()
        server.sendToAllPlayers(createServerInfoMessage("Contest is being cancelled after $duration."))
        events.send(ContestEvent(ContestEventType.STOP, 0))
    }

    private fun Int.toMilliseconds(): Long = this * 60_000L

    /**
     * Initializes the events. The order does not matter, but the delay does.
     */
    private suspend fun initEvents() {
        // start and stop events
        events.send(ContestEvent(ContestEventType.START, 0))
        events.send(ContestEvent(ContestEventType.STOP, configuration.durationInMinutes))
        // Report events
        var currentMinute = configuration.statusReportIntervalInMinutes
        while (currentMinute < configuration.durationInMinutes) {
            events.send(ContestEvent(ContestEventType.REPORT, currentMinute))
            currentMinute += configuration.statusReportIntervalInMinutes
        }
        // Additional events
        configuration.additionalEvents.forEach { events.send(it) }
    }

    private fun processEvents(): Job = launch {
        for (event: ContestEvent in events) {
            launch {
                delay(event.delayInMinutes.toMilliseconds())
                event.processNow()
            }
        }
    }

    internal suspend fun intermediateReport() {
        events.send(ContestEvent(ContestEventType.REPORT, 0))
    }

    private suspend fun ContestEvent.processNow() {
        when (type) {
            ContestEventType.START -> startContest()
            ContestEventType.REPORT -> report()
            ContestEventType.SPAWN_FRENZY -> spawnFrenzy()
            ContestEventType.DESPAWN_FRENZY -> despawnFrenzy()
            ContestEventType.STOP -> stopContest()
        }
    }

    private suspend fun startContest() {
        if (!everStarted) {
            server.commandExecutor.addCommand(StopCommand(server, true))
            server.commandExecutor.addCommand(ClearCommand(server))
            everStarted = true
            contestStartedAt = System.currentTimeMillis()
            val contestStartMessage = "Starting contest. Contest will run for ${configuration.durationInMinutes} minutes."
            LOGGER.info(contestStartMessage)
            server.sendToAllPlayers(createServerInfoMessage(contestStartMessage).thereIsMore())
            server.commandExecutor.addCommand(GoCommand(server))
        } else {
            LOGGER.error("Contest is already started or already over.")
        }
    }

    private suspend fun report() {
        if (contestRunning) {
            val now = System.currentTimeMillis()
            val running: Long = ((now - contestStartedAt) / 1000L) * 1000L
            val remaining: Long = configuration.durationInMinutes.toMilliseconds() - running
            val runningString: String = running.milliseconds.toString()
            val remainingString: String = remaining.milliseconds.toString()
            server.sendToAllPlayers(createServerInfoMessage("Contest is running for $runningString.").thereIsMore())
            server.sendToAllPlayers(createServerInfoMessage("Contest will run for another $remainingString."))
        } else {
            LOGGER.error("No report: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun spawnFrenzy() {
        if (contestRunning) {
            server.frenzyHandler.spawnManually(true)
        } else {
            LOGGER.error("No spawn frenzy: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun despawnFrenzy() {
        if (contestRunning) {
            server.frenzyHandler.despawn()
        } else {
            LOGGER.error("No despawn frenzy: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun stopContest() {
        server.commandExecutor.addCommand(StopCommand(server, true))
        val topList: List<ServerPlayer> = server.getAllPlayingPlayerConnections().asSequence()
            .filter { it.status == ClientConnectionStatus.PLAYING }
            .map { it.player }
            .sortedByDescending { it.score }
            .take(configuration.statusPositions)
            .toList()
        var position = 1
        for (player in topList) {
            server.sendToAllPlayers(createServerInfoMessage("Position ${String.format("%02d", position)}: ${player.nick}").thereIsMore())
            position += 1
        }
        server.sendToAllPlayers(createServerInfoMessage("The winner is: ${topList.first().nick}"))
        eventJob.cancel()
        server.contestController = null
    }

}