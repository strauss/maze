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

package de.dreamcube.mazegame.server.contest

import de.dreamcube.mazegame.common.api.GameSpeed
import de.dreamcube.mazegame.common.api.GameSpeed.*
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.Player
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.control.ClearCommand
import de.dreamcube.mazegame.server.maze.commands.control.GoCommand
import de.dreamcube.mazegame.server.maze.commands.control.StopCommand
import de.dreamcube.mazegame.server.maze.commands.game.BaitRushCommand
import de.dreamcube.mazegame.server.maze.commands.game.TransformBaitsCommand
import de.dreamcube.mazegame.server.maze.createEmptyLastMessage
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds

private const val NO_DELAY = 0.0

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

    lateinit var initialSpeed: GameSpeed

    fun start() = launch(start = CoroutineStart.UNDISPATCHED) {
        initEvents()
        initialSpeed = server.gameSpeed
        server.changeSpeed(configuration.initialGameSpeed)
        eventJob = processEvents()
    }

    suspend fun stop() {
        val now = System.currentTimeMillis()
        val duration: String = (now - contestStartedAt).milliseconds.toString()
        server.sendToAllPlayers(createServerInfoMessage("Contest is being cancelled after $duration."))
        events.send(ContestEvent(ContestEventType.STOP, NO_DELAY))
    }

    private fun Double.toMilliseconds(): Long = round(this * 60_000.0).toLong()

    /**
     * Initializes the events. The order does not matter, but the delay does.
     */
    private suspend fun initEvents() {
        // start and stop events
        events.send(ContestEvent(ContestEventType.START, NO_DELAY))
        events.send(ContestEvent(ContestEventType.STOP, configuration.durationInMinutes))
        // Report events
        val reportInterval = configuration.statusReportIntervalInMinutes
            .coerceAtLeast(0.1) // no "spam"
            .coerceAtMost(configuration.durationInMinutes / 3.0) // at least three reports

        var currentMinute = reportInterval
        while (currentMinute < configuration.durationInMinutes) {
            events.send(ContestEvent(ContestEventType.REPORT, currentMinute))
            currentMinute += reportInterval
        }

        // Event sets
        configuration.eventSets.asSequence()
            .flatMap { it.toEventList(configuration.durationInMinutes) }
            .forEach { events.send(it) }

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
        events.send(ContestEvent(ContestEventType.REPORT, NO_DELAY))
    }

    private suspend fun ContestEvent.processNow() {
        when (type) {
            ContestEventType.START -> startContest()
            ContestEventType.REPORT -> report()
            ContestEventType.SPAWN_FRENZY -> spawnFrenzy()
            ContestEventType.DESPAWN_FRENZY -> despawnFrenzy()
            ContestEventType.SPEED_UP -> speedUp()
            ContestEventType.SLOW_DOWN -> slowDown()
            ContestEventType.ALL_TRAPS -> transformAllBaits(BaitType.TRAP)
            ContestEventType.ALL_FOOD -> transformAllBaits(BaitType.FOOD)
            ContestEventType.ALL_COFFEE -> transformAllBaits(BaitType.COFFEE)
            ContestEventType.ALL_GEMS -> transformAllBaits(BaitType.GEM)
            ContestEventType.BAIT_RUSH -> baitRush()
            ContestEventType.RE_BAIT -> regenerateBaits()
            ContestEventType.SHUFFLE_PLAYERS -> shufflePlayers()
            ContestEventType.STOP -> stopContest()
        }
    }

    private suspend fun startContest() {
        if (!everStarted) {
            server.commandExecutor.addCommand(StopCommand(server, true))
            server.commandExecutor.addCommand(ClearCommand(server))
            everStarted = true
            contestStartedAt = System.currentTimeMillis()
            val contestStartMessage =
                "Starting contest. Contest will run for ${configuration.durationInMinutes} minutes."
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

    private suspend fun speedUp() {
        if (contestRunning) {
            server.changeSpeed(server.gameSpeed.speedUp())
            server.sendToAllPlayers(createServerInfoMessage("Let's speed things up a little."))
        } else {
            LOGGER.error("No speedup: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun slowDown() {
        if (contestRunning) {
            server.changeSpeed(server.gameSpeed.slowDown())
            server.sendToAllPlayers(createServerInfoMessage("That was exhausting. Slowing down."))
        } else {
            LOGGER.error("No slowdown: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun transformAllBaits(into: BaitType) {
        if (contestRunning) {
            server.commandExecutor.addCommand(TransformBaitsCommand(server, into))
        } else {
            LOGGER.error("No bait transform: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun baitRush() {
        if (contestRunning) {
            server.commandExecutor.addCommand(BaitRushCommand(server))
        } else {
            LOGGER.error("No bait rush: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun regenerateBaits() {
        if (contestRunning) {
            server.commandExecutor.addCommand {
                val messages = buildList {
                    val formerDesiredBaitCount = server.desiredBaitCount.get()
                    server.desiredBaitCount.set(0)
                    addAll(server.withdrawBaits())
                    server.desiredBaitCount.set(formerDesiredBaitCount)
                    addAll(server.fillBaits())
                    add(createServerInfoMessage("What? Let's pretend nobody noticed..."))
                }
                server.sendToAllPlayers(*messages.toTypedArray())
            }
        } else {
            LOGGER.error("No bait regeneration: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun shufflePlayers() {
        if (contestRunning) {
            server.commandExecutor.addCommand {
                val messages = buildList {
                    add(createServerInfoMessage("Wait what? Something is malfunctioning ... where are y'all going?"))
                    server.getAllPlayingPlayerConnections().asSequence()
                        .filter { it.status == ConnectionStatus.PLAYING } // no spectators
                        .map { it.player }
                        .forEach {
                            add(server.teleportPlayerRandomly(it, trap = false).thereIsMore())
                        }
                    add(createEmptyLastMessage())
                }
                server.sendToAllPlayers(*messages.toTypedArray())
            }
        } else {
            LOGGER.error("No player shuffling: $CONTEST_OVER_OR_NOT_STARTED_MESSAGE")
        }
    }

    private suspend fun stopContest() {
        if (isActive) {
            server.commandExecutor.addCommand(StopCommand(server, true))
            if (initialSpeed != server.gameSpeed) {
                // we restore the speed if it differs
                server.changeSpeed(initialSpeed)
            }
            val topList: List<Player> = server.getAllPlayingPlayerConnections().asSequence()
                .filter { it.status == ConnectionStatus.PLAYING }
                .map { it.player }
                .sortedByDescending { it.score }
                .take(configuration.statusPositions)
                .toList()
            var position = 1
            for (player in topList) {
                server.sendToAllPlayers(
                    createServerInfoMessage("Position ${String.format("%02d", position)}: ${player.nick}").thereIsMore()
                )
                position += 1
            }
            server.sendToAllPlayers(createServerInfoMessage("The winner is: ${topList.first().nick}"))
            eventJob.cancel()
            server.contestController = null
        } else {
            LOGGER.error(CONTEST_OVER_OR_NOT_STARTED_MESSAGE)
        }
    }

    private fun GameSpeed.speedUp(): GameSpeed = when (this) {
        UNLIMITED -> UNLIMITED
        ULTRA -> UNLIMITED
        FAST -> ULTRA
        NORMAL -> FAST
        SLOW -> NORMAL
        ULTRA_SLOW -> SLOW
    }

    private fun GameSpeed.slowDown(): GameSpeed = when (this) {
        UNLIMITED -> ULTRA
        ULTRA -> FAST
        FAST -> NORMAL
        NORMAL -> SLOW
        SLOW -> ULTRA_SLOW
        ULTRA_SLOW -> ULTRA_SLOW
    }

}