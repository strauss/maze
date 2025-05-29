package mazegame.server_ktor.maze

import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class BaitGenerator(private val server: MazeServer) {

    private val random = Random.Default

    companion object {
        const val FOOD_TH = 0.4
        const val COFFEE_TH = 0.8
        const val GEM_TH = 0.9
        const val NO_TRAP_FOOD_TH = 0.45
        const val NO_TRAP_COFFEE_TH = 0.9
    }

    /**
     * Creates a random bait type including traps.
     */
    fun createRandomBaitType(): BaitType {
        val roll: Double = random.nextDouble()
        return when {
            roll < FOOD_TH -> BaitType.FOOD
            roll < COFFEE_TH -> BaitType.COFFEE
            roll < GEM_TH -> BaitType.GEM
            else -> BaitType.TRAP
        }
    }

    /**
     * Creates a random bait type excluding traps.
     */
    fun createRandomBaitTypeNoTrap(): BaitType {
        val roll: Double = random.nextDouble()
        return when {
            roll < NO_TRAP_FOOD_TH -> BaitType.FOOD
            roll < NO_TRAP_COFFEE_TH -> BaitType.COFFEE
            else -> BaitType.GEM
        }
    }

    /**
     * Creates a random bait.
     */
    suspend fun createRandomBait(): ServerBait {
        val newBaitType = if (server.currentTrapCount.get() < server.maxTrapCount) {
            createRandomBaitType()
        } else {
            createRandomBaitTypeNoTrap()
        }
        return when (newBaitType) {
            BaitType.FOOD -> createRandomFood()
            BaitType.COFFEE -> createRandomCoffee()
            BaitType.GEM -> createRandomGem()
            BaitType.TRAP -> createRandomTrap()
        }
    }

    /**
     * Creates a random food bait with complete random distribution.
     */
    private suspend fun createRandomFood(): ServerBait {
        val p: Position = server.positionProvider.randomFreePosition()
        return ServerBait(BaitType.FOOD, p.x, p.y)
    }

    /**
     * Creates a random coffee bait with complete random distribution.
     */
    private suspend fun createRandomCoffee(): ServerBait {
        val p: Position = server.positionProvider.randomFreePosition()
        return ServerBait(BaitType.COFFEE, p.x, p.y)
    }

    /**
     * Creates a random gem bait with positions preferring hallways and dead ends. Some of them will spawn invisible.
     */
    private suspend fun createRandomGem(): ServerBait {
        val p: Position = server.positionProvider.randomPositionForGem()
        val gem = ServerBait(BaitType.GEM, p.x, p.y)
        // approx 15% of the gems spawn invisible
        val spawnInvisible: Boolean = random.nextDouble() > 0.85
        if (spawnInvisible) {
            gem.makeInvisible(5_000L)
        }
        return gem
    }

    /**
     * Creates a random trap bait with positions preferring junctions. Half of them will spawn invisible.
     */
    private suspend fun createRandomTrap(): ServerBait {
        val p: Position = server.positionProvider.randomPositionForTrap()
        val trap = ServerBait(BaitType.TRAP, p.x, p.y)
        // approx 50% of the traps spawn invisible
        val spawnInvisible: Boolean = random.nextDouble() > 0.5
        if (spawnInvisible) {
            trap.makeInvisible(20_000L)
        }
        return trap
    }

    internal suspend fun findInvisibleBaitsThatShouldReappear(): List<ServerBait> {
        // We first get all currently invisible baits
        val invisibleBaits: List<ServerBait> = buildList {
            server.baitMutex.withLock {
                for (currentBait in server.baitsById.values) {
                    if (!currentBait.visibleToClients) {
                        add(currentBait)
                    }
                }
            }
        }
        // We check if the invisible baits should reappear. If so, we make them visible again, and add them to the result list.
        return buildList {
            for (currentBait in invisibleBaits) {
                currentBait.checkReappear()
                if (currentBait.visibleToClients) {
                    add(currentBait)
                }
            }
        }
    }
}