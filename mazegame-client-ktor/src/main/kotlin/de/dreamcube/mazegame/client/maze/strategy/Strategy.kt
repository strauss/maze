package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.client.maze.createStepMessage
import de.dreamcube.mazegame.client.maze.createTurnLeftMessage
import de.dreamcube.mazegame.client.maze.createTurnRightMessage
import de.dreamcube.mazegame.client.maze.events.*
import de.dreamcube.mazegame.client.maze.events.EventListener
import de.dreamcube.mazegame.common.maze.isNickValid
import de.dreamcube.mazegame.common.maze.sanitizeAsFlavorText
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
import javax.swing.JPanel
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
 * - [ClientConnectionStatusListener] for reacting to connection status events
 * - [SpeedChangedListener] for reacting to changes in game speed
 *
 * The strategy is automatically registered for all relevant event listeners when the client is initialized.
 * See [EventHandler.addEventListener] for details.
 */
abstract class Strategy : NoEventListener {

    companion object {

        private val LOGGER: Logger = LoggerFactory.getLogger(Strategy::class.java)

        private val strategyMutex = Mutex()

        /**
         * Map containing all strategies.
         */
        private val STRATEGIES: MutableMap<String, Class<out Strategy>> = HashMap()

        /**
         * Set containing all spectator strategies.
         */
        private val SPECTATOR_NAMES: MutableSet<String> = HashSet()

        /**
         * Set containing all human strategies.
         */
        private val HUMAN_NAMES: MutableSet<String> = HashSet()

        /**
         * Map containing all flavor texts.
         */
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
                        if (!isNickValid(strategyName)) {
                            LOGGER.error("Strategy '$strategyName' has an invalid name and will be ignored. The name has to start with a letter and may only contain letters, digits, '_', and '-'. No fancy stuff!")
                            continue
                        }
                        val spectator: Boolean = it.isSpectator
                        val human: Boolean = it.isHuman
                        val registeredStrategy = STRATEGIES[strategyName]
                        if (registeredStrategy != null) {
                            LOGGER.warn("Strategy '$strategyName' has already been registered for class '${registeredStrategy.canonicalName}' and will be skipped.")
                            continue
                        }
                        if (spectator) {
                            SPECTATOR_NAMES.add(strategyName)
                        }
                        if (human) {
                            HUMAN_NAMES.add(strategyName)
                        }
                        LOGGER.info("Strategy '$strategyName' will be registered for class '${strategyClass.canonicalName}'.")
                        STRATEGIES[strategyName] = strategyClass
                        FLAVOR_TEXTS[strategyName] = it.flavor.sanitizeAsFlavorText()
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

    lateinit var mazeClient: MazeClient
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
            val waitFor = max(mazeClient.gameSpeed, 10)
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
     * With this function, you can provide a control panel. This panel is placed on the right side. You can use it
     * for controlling bot parameters at runtime or, for displaying additional information. If you plan on creating a
     * visualization, you don't need to put a button for it in here, the UI will do it for you.
     *
     * Although you provide a UI element with this method, never open up your own UI elements directly. The bot is
     * intended to run headless (without UI) and embedded into a UI. If you randomly open up windows, the bot won't be
     * able to run inside a server.
     *
     * If you don't want or need a control panel, just leave the function alone. It will return null and therefore not
     * enable the control panel for your bot.
     */
    open fun getControlPanel(): JPanel? {
        return null
    }

    /**
     * This function allows you for providing a visualization for your bot. If left alone, null is returned and your
     * bot won't have a visualization.
     */
    open fun getVisualizationComponent(): VisualizationComponent? {
        return null
    }

    /**
     * Override this method to initialize the custom strategy. Does nothing in the superclass.
     */
    protected open fun initializeStrategy() {
        // still does nothing ... and will always be good at it!
    }

    /**
     * Override this method to perform an action right before the "BYE!" command is sent. Maybe you could literally
     * say "goodbye" :-)
     */
    open fun beforeGoodbye() {
        // does nothing by default
    }

    /**
     * This method must decide what to do next and return the appropriate [Move] literal.
     */
    protected abstract fun getNextMove(): Move

}