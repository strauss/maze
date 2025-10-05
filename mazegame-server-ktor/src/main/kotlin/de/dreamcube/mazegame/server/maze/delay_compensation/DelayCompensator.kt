package de.dreamcube.mazegame.server.maze.delay_compensation

import de.dreamcube.mazegame.common.util.AverageCalculator
import de.dreamcube.mazegame.common.util.SimpleMovingAverageCalculator
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

private const val DELAY_WINDOW_SIZE: Int = 21
private const val COMMAND_WINDOW_SIZE: Int = 5
private const val TOLERACNE_MS: Long = 5L

class DelayCompensator(private val clientConnection: ClientConnection, private val server: MazeServer) {

    companion object {
        private const val MIN_PENALTY_FRACTION: Double = -2.0 / 3.0
        private const val MAX_PENALTY_FRACTION: Double = 20.0 / 3.0
        private val LOGGER = LoggerFactory.getLogger(DelayCompensator::class.java)
    }

    private enum class InternalState {
        INITIALIZED, STARTED, STOPPED
    }

    private val mutex = Mutex()
    private var internalState: InternalState = InternalState.INITIALIZED

    val maxCompensation: Long
        get() = server.gameSpeed.delay - 25L

    var penaltyTime: Long = 0L
        internal set(penaltyTime) {
            val delay: Long = server.gameSpeed.delay
            val minPenalty: Long = round(delay * MIN_PENALTY_FRACTION).toLong()
            val maxPenalty: Long = round(delay * MAX_PENALTY_FRACTION).toLong()
            field = max(minPenalty, penaltyTime)
            field = min(field, maxPenalty)
        }

    private val lastReady = AtomicLong(-1L)
    private val lastCommandTime = AtomicLong(-1)
    private val commandsReceived = AtomicLong(0L)
    private val averageDelay: AverageCalculator<Long> = SimpleMovingAverageCalculator(DELAY_WINDOW_SIZE)
    private val averageCommandDelta: AverageCalculator<Long> = SimpleMovingAverageCalculator(COMMAND_WINDOW_SIZE)

    suspend fun getTurnTimeOffset(): Long = mutex.withLock {
        val averageTime = averageDelay.average
        val compensationTime = max(min(maxCompensation, averageTime), 0L)
        return penaltyTime - compensationTime
    }

    suspend fun stopTimer() = mutex.withLock {
        if (internalState == InternalState.STARTED) {
            val time = System.currentTimeMillis() - lastReady.get()
            // ignore extreme values
            if (time < maxCompensation) {
                averageDelay.addValue(time)
                handleCommandMeasurement()
            }
        }
        internalState = InternalState.STOPPED
        commandsReceived.incrementAndGet()
    }

    suspend fun handleCommandMeasurement() {
        val currentCommandTime: Long = System.currentTimeMillis()
        if (lastCommandTime.get() > 0) {
            val commandDeltaTime = currentCommandTime - lastCommandTime.get()
            averageCommandDelta.addValue(commandDeltaTime)
            val avgCommandDelta: Long = averageCommandDelta.average
            if (commandsReceived.get() > DELAY_WINDOW_SIZE && avgCommandDelta + TOLERACNE_MS < server.gameSpeed.delay) {
                // ignore dummys for logging
                if (!clientConnection.nick.startsWith(server.serverConfiguration.serverBots.specialBots.dummy)) {
                    LOGGER.warn("${clientConnection.nick}: cmds ${commandsReceived.get()} : avgc $avgCommandDelta : lstc $commandDeltaTime : avgd ${averageDelay.average}")
                }
                resetTimer()
                if (avgCommandDelta + TOLERACNE_MS * 2 < server.gameSpeed.delay) {
                    clientConnection.sendMessage(createServerInfoMessage("Your last command was received too quickly. Your delay compensation has been reset."))
                }
            }
        }
        lastCommandTime.set(currentCommandTime)
    }

    fun resetTimer() {
        lastReady.set(-1)
        averageDelay.reset()
        averageCommandDelta.reset()
        commandsReceived.set(0L)
        internalState = InternalState.INITIALIZED
    }

    suspend fun startTimer() = mutex.withLock {
        internalState = InternalState.STARTED
        lastReady.set(System.currentTimeMillis())
    }
}
