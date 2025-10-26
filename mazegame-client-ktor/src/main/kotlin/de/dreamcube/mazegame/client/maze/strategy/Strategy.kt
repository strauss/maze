package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.createStepMessage
import de.dreamcube.mazegame.client.maze.createTurnLeftMessage
import de.dreamcube.mazegame.client.maze.createTurnRightMessage
import de.dreamcube.mazegame.client.maze.events.*
import de.dreamcube.mazegame.client.maze.events.EventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

/**
 * Abstract superclass for all strategies. For initializing the strategy with data and receiving data at runtime, several [EventListener] interfaces
 * should be implemented. Usually only a few of them are required, but it depends on the strategy.
 *
 * Here a short overview:
 * - [MazeEventListener] for receiving maze-related events. Currently only contains one event for receiving the map data, essential for creating
 * custom maze representations.
 * - [BaitEventListener] for bait-related data (appear and vanish)
 * - [PlayerConnectionListener] for events related to players joining and leaving the game
 * - [PlayerMovementListener] for events related to players changing position (including appearing and vanishing from the map)
 * - [ScoreChangeListener] for score-related events
 * - [ChatInfoListener] for chat messages, including whispers (allows for bots to talk with each other)
 * - [ErrorInfoListener] for server error messages (e.g., allows for reacting on wall crashes, like in [Aimless])
 *
 * The strategy is automatically registered for all relevant event listeners when the client is initialized.
 * See [EventHandler.addEventListener] for details.
 */
abstract class Strategy : NoEventListener {

    companion object {

        private val LOGGER: Logger = LoggerFactory.getLogger(Strategy::class.java)

        private val strategyMutex = Mutex()

        private val STRATEGIES: MutableMap<String, Class<out Strategy>> = HashMap()

        private val SPECTATOR_NAMES: MutableSet<String> = HashSet()

        private val HUMAN_NAMES: MutableSet<String> = HashSet()

        private val FLAVOR_TEXTS: MutableMap<String, String> = HashMap()

        @JvmStatic
        @JvmName("scanAndAddStrategies")
        fun scanAndAddStrategiesBlocking() = runBlocking { scanAndAddStrategies() }

        suspend fun scanAndAddStrategies() {
            // Scan for classes using the reflections library
            val reflections = Reflections(ConfigurationBuilder().setUrls(ClasspathHelper.forJavaClassPath()))
            val foundStrategies: Set<Class<out Strategy>> = reflections.getSubTypesOf(Strategy::class.java)

            strategyMutex.withLock {
                for (strategyClass: Class<out Strategy> in foundStrategies) {
                    val botAnnotation: Bot? = strategyClass.getAnnotation(Bot::class.java)
                    botAnnotation?.let {
                        val strategyName: String = it.value
                        val spectator: Boolean = it.isSpectator
                        val human: Boolean = it.isHuman
                        val registeredStrategy = STRATEGIES[strategyName]
                        if (registeredStrategy != null) {
                            LOGGER.warn("Strategy '$strategyName' has already been registered for class '${registeredStrategy.canonicalName}' and will be skipped.")
                            return@let // not quite a 'continue'
                        }
                        if (spectator) {
                            SPECTATOR_NAMES.add(strategyName)
                        }
                        if (human) {
                            HUMAN_NAMES.add(strategyName)
                        }
                        LOGGER.info("Strategy '$strategyName' will be registered for class '${strategyClass.canonicalName}'.")
                        STRATEGIES[strategyName] = strategyClass
                        FLAVOR_TEXTS[strategyName] = it.flavor
                    }
                }
            }
        }

        @JvmStatic
        @JvmName("createStrategy")
        fun createStrategyBlocking(strategyName: String): Strategy? = runBlocking { createStrategy(strategyName) }

        suspend fun createStrategy(strategyName: String): Strategy? {
            val strategyClass = strategyMutex.withLock { STRATEGIES[strategyName] }
            if (strategyClass != null) {
                try {
                    val instance = strategyClass.getConstructor().newInstance()
                    LOGGER.info("Successfully created instance for '$strategyName' using class '${strategyClass.canonicalName}'.")
                    return instance
                } catch (ex: Exception) {
                    LOGGER.error("Could not instantiate '${strategyClass.canonicalName}': ${ex.message}", ex)
                }
            }
            return null
        }

        @JvmStatic
        @JvmName("getStrategyNames")
        fun getStrategyNamesBlocking(): Set<String> = runBlocking { getStrategyNames() }

        suspend fun getStrategyNames(): Set<String> {
            strategyMutex.withLock {
                return Collections.unmodifiableSet(TreeSet(STRATEGIES.keys))
            }
        }

        @JvmStatic
        @JvmName("isHumanStrategy")
        fun String.isHumanStrategyBlocking(): Boolean {
            val name = this
            return runBlocking { name.isHumanStrategy() }
        }

        suspend fun String.isHumanStrategy(): Boolean {
            strategyMutex.withLock {
                return this in HUMAN_NAMES
            }
        }

        @JvmStatic
        @JvmName("isSpectatorStrategy")
        fun String.isSpectatorStrategyBlocking(): Boolean {
            val name = this
            return runBlocking { name.isSpectatorStrategy() }
        }

        suspend fun String.isSpectatorStrategy(): Boolean {
            strategyMutex.withLock {
                return this in SPECTATOR_NAMES
            }
        }

        @JvmStatic
        @JvmName("isBotStrategy")
        fun String.isBotStrategyBlocking(): Boolean {
            val name = this
            return runBlocking { name.isBotStrategy() }
        }

        suspend fun String.isBotStrategy(): Boolean {
            strategyMutex.withLock {
                return this !in HUMAN_NAMES && this !in SPECTATOR_NAMES
            }
        }

        @JvmStatic
        @JvmName("flavorText")
        fun String.flavorTextBlocking(): String {
            val name = this
            return runBlocking { name.flavorText() }
        }

        suspend fun String.flavorText(): String {
            strategyMutex.withLock {
                return FLAVOR_TEXTS[this] ?: ""
            }
        }

    }

    protected lateinit var mazeClient: MazeClient
        private set

    /**
     * Used to artificially slow down the bot. Mostly useful for server-side bots, due to the delay compensation.
     */
    protected var botDelayInMs: Int = 0

    internal fun initClient(mazeClient: MazeClient) {
        this.mazeClient = mazeClient
        mazeClient.eventHandler.addEventListener(this)
        initializeStrategy()
    }

    internal suspend fun makeNextMove() {
        // call strategy
        val move: Move = getNextMove()
        if (move == Move.DO_NOTHING) {
            val waitFor = max(mazeClient.gameSpeed, mazeClient.ownPlayer.moveTime.toInt())
            mazeClient.scope.launch {
                delay(waitFor.toLong())
                makeNextMove()
            }
        } else if (botDelayInMs == 0) {
            execute(move)
        } else {
            mazeClient.scope.launch {
                delay(botDelayInMs.toLong())
                execute(move)
            }
        }
    }

    private suspend fun execute(move: Move) {
        if (!mazeClient.isLoggedIn) {
            return
        }
        when (move) {
            Move.STEP -> mazeClient.sendMessage(createStepMessage())
            Move.TURN_L -> mazeClient.sendMessage(createTurnLeftMessage())
            Move.TURN_R -> mazeClient.sendMessage(createTurnRightMessage())
            Move.DO_NOTHING -> {
                // literally do nothing
            }
        }
    }

    /**
     * Override this method to initialize the custom strategy. Does nothing in the superclass. The method is not abstract for backwards compatibility
     * reasons.
     */
    protected fun initializeStrategy() {
        // still does nothing ... and will always be good at it!
    }

    /**
     * Is called before the strategy is terminated by the client.
     */
    protected fun beforeGoodBye() {
        // does nothing by default
    }

    /**
     * This method must decide what to do next and return the appropriate [Move] literal.
     */
    protected abstract fun getNextMove(): Move

}