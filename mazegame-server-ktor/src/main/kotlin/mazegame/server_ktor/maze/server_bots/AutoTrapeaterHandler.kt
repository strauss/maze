package mazegame.server_ktor.maze.server_bots

import kotlinx.coroutines.launch
import mazegame.server_ktor.maze.ClientConnection
import mazegame.server_ktor.maze.GameSpeed
import mazegame.server_ktor.maze.MazeServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.round
import kotlin.random.Random

/**
 * Handles the server-side auto trapeater if it is enabled.
 */
class AutoTrapeaterHandler(mazeServer: MazeServer) : ServerBotHandler(mazeServer) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AutoTrapeaterHandler::class.java)

        /**
         * Determines the speed penalty a trapeater receives, when no traps are present. It is a fraction of the current delay that is determined by
         * the current [GameSpeed]. A value of 1.0 doubles the delay and halves the speed.
         */
        private const val NORMAL_PENALTY_FRACTION: Double = 1.4  // 210 @ 150

        /**
         * Determines the penalty reduction per visible trap. It is also a fraction of the current delay.
         */
        private const val PENALTY_REDUCTION_PER_TRAP: Double = 0.2 // 30 @ 150

        /**
         * After a despawn, this many ms have to pass until the trapeater is allowed to spawn again.
         */
        private const val SPAWN_COOLDOWN_MS: Long = 1000L * 60L * 2L // 2 Minutes

        /**
         * After a spawn, this many ms have to pass until the trapeater is allowed to despawn again.
         */
        private const val DESPAWN_COOLDOWN_MS: Long = 1000L * 60L * 5L // 5 Minutes
    }

    private val random: Random = Random.Default

    override val botAlias: String
        get() = mazeServer.serverConfiguration.serverBots.specialBots.trapeater

    /**
     * Earliest time of the next trapeater spawn
     */
    var doNotSpawnBefore: Long

    /**
     * Is spawning allowed?
     */
    val spawnEnabled: Boolean
        get() = System.currentTimeMillis() > doNotDespawnBefore

    /**
     * Is spawning considered?
     */
    val trapsOverSpawnThreshold: Boolean
        get() = mazeServer.currentTrapCount.get() >= mazeServer.maxTrapCount

    /**
     * Earliest time of the next trapeater despawn
     */
    var doNotDespawnBefore: Long

    /**
     * Is despawning allowed?
     */
    val despawnEnabled: Boolean
        get() = System.currentTimeMillis() > doNotDespawnBefore

    /**
     * Is despawning considered?
     */
    val trapsBelowDespawnThreshold: Boolean
        get() = mazeServer.visibleTrapCount.get() <= mazeServer.maxTrapCount / 3

    init {
        val now: Long = System.currentTimeMillis()
        doNotSpawnBefore = now + SPAWN_COOLDOWN_MS
        doNotDespawnBefore = now + DESPAWN_COOLDOWN_MS
    }

    /**
     * Checks if a trapeater should be spawned or despawned. If so, it spawned or despawned. If a trapeater is running, its speed is adjusted.
     */
    override suspend fun handle() {
        if (!active && spawnEnabled && trapsOverSpawnThreshold) {
            // Check for spawn
            val roll: Double = random.nextDouble()
            if (roll < 0.1) {
                mazeServer.scope.launch { spawn() }
            }
        } else if (active && despawnEnabled && trapsBelowDespawnThreshold) {
            // Check for despawn
            val roll: Double = random.nextDouble()
            if (roll < 0.1) {
                mazeServer.scope.launch { despawn() }
            }
        }

        // Finally adjust trapeater's speed
        if (active) {
            val penaltyTime: Long = computeTrapeaterPenaltyTime()
            val connection: ClientConnection? = mazeServer.getClientConnection(client?.clientId)
            val oldValue = connection?.delayCompensator?.penaltyTime
            if (oldValue != penaltyTime) {
                connection?.delayCompensator?.penaltyTime = penaltyTime
            }
        }
    }

    /**
     * Sets the [doNotSpawnBefore] timestamp. If the trapeater could not be spawned, the [mazeServer] is told to not try it again.
     */
    override suspend fun postSpawn() {
        doNotDespawnBefore = System.currentTimeMillis() + DESPAWN_COOLDOWN_MS
        if (client == null) {
            LOGGER.error("Trapeater '$botAlias' could not be spawned. Auto-trapeater option will be deactivated.")
            mazeServer.autoTrapeaterEnabled = false
        }
    }

    override suspend fun postDespawn() {
        doNotSpawnBefore = System.currentTimeMillis() + SPAWN_COOLDOWN_MS
    }

    /**
     * Determines the new penalty time of the trapeater based on the currently visible fields.
     */
    private fun computeTrapeaterPenaltyTime(): Long {
        val currentDelay = mazeServer.gameSpeed.delay
        val normalTrapeaterPenalty: Long = round(currentDelay.toDouble() * NORMAL_PENALTY_FRACTION).toLong()
        val penaltyDecreasePerTrap: Long = round(currentDelay.toDouble() * PENALTY_REDUCTION_PER_TRAP).toLong()
        return normalTrapeaterPenalty - (penaltyDecreasePerTrap * mazeServer.visibleTrapCount.get())
    }

}