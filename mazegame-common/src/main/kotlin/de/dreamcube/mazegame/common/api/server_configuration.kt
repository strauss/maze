package de.dreamcube.mazegame.common.api

import com.fasterxml.jackson.annotation.JsonIgnore

data class ConnectionDto(
    val port: Int = 0,
    val maxClients: Int = 20,
    val loginTimeout: Long = 30_000L,
    val instantFlush: Boolean = false
)

data class GeneratorParametersDto(
    val width: Int = 40,
    val height: Int = 30,
    val mapFile: String? = null,
    val templateFillStartPoints: Boolean = false
)

data class MazeGeneratorConfigurationDto(
    val generatorMode: GeneratorMode = GeneratorMode.RANDOM,
    val generatorParameters: GeneratorParametersDto = GeneratorParametersDto()
)

data class SpecialBotsDto(
    val dummy: String = "dummy",
    val trapeater: String = "trapeater",
    val frenzy: String = "frenzy",
    val spectator: String = "spectator"
)

data class FreeNickMapping(val botName: String, private val additionalNames: Set<String> = setOf()) {
    val nickNames: Set<String>
        get() = additionalNames + botName
}

data class NickMappingsDto(
    val dummyNames: Set<String>,
    val trapeaterNames: Set<String>,
    val frenzyNames: Set<String>,
    val freeNickMappings: List<FreeNickMapping> = listOf()
)

data class ServerBotsDto(
    val autoLaunch: List<String> = listOf(),
    val specialBots: SpecialBotsDto = SpecialBotsDto(),
    val nickMappings: NickMappingsDto = NickMappingsDto(
        setOf(specialBots.dummy),
        setOf(specialBots.trapeater),
        setOf(specialBots.frenzy)
    )
) {
    @JsonIgnore
    val specialBotNames: Set<String> = nickMappings.dummyNames + nickMappings.trapeaterNames + nickMappings.frenzyNames
}

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