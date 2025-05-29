package mazegame.server_ktor.config

import mazegame.server_ktor.maze.GameSpeed
import mazegame.server_ktor.maze.generator.GeneratorMode

data class ConnectionDto(val port: Int = 0, val maxClients: Int = 20, val loginTimeout: Long = 30_000L, val instantFlush: Boolean = false)
data class GeneratorParametersDto(val width: Int = 40, val height: Int = 30, val mapFile: String? = null)
data class MazeGeneratorConfigurationDto(
    val generatorMode: GeneratorMode = GeneratorMode.RANDOM,
    val generatorParameters: GeneratorParametersDto = GeneratorParametersDto()
)

data class SpecialBotsDto(
    val dummy: String = "dummy",
    val trapeater: String = "trapeater",
    val frenzy: String = "frenzy",
    val spectator: String = "spectator"
) {
    private val specialBots = listOf(dummy, trapeater, frenzy, spectator)
    fun isSpecial(nick: String): Boolean {
        for (it in specialBots) {
            if (nick.startsWith(it)) {
                return true
            }
        }
        return false
    }
}

data class ServerBotsDto(val autoLaunch: List<String> = listOf(), val specialBots: SpecialBotsDto = SpecialBotsDto())
data class BaitGeneratorDto(val objectDivisor: Int = 26, val trapDivisor: Int = 4)
data class SpecialEventsDto(
    val enabled: Boolean = false,
    val eventCooldown: Long = 90_000,
    val allTrapProbability: Double = 0.01,
    val allFoodProbability: Double = 0.01,
    val allCoffeeProbability: Double = 0.01,
    val allGemProbability: Double = 0.005,
    val baitRushProbability: Double = 0.05,
    val loseBaitProbability: Double = 0.20
)

data class GameDto(
    val initialSpeed: GameSpeed = GameSpeed.NORMAL,
    val generateBaitsAtStart: Boolean = true,
    val autoTrapeater: Boolean = true,
    val allowSpectator: Boolean = true,
    val delayCompensation: Boolean = true,
    val baitGenerator: BaitGeneratorDto = BaitGeneratorDto(),
    val events: SpecialEventsDto = SpecialEventsDto()
)

class MazeServerConfigurationDto(
    val connection: ConnectionDto = ConnectionDto(),
    val maze: MazeGeneratorConfigurationDto = MazeGeneratorConfigurationDto(),
    val serverBots: ServerBotsDto = ServerBotsDto(),
    val game: GameDto = GameDto()
)