package de.dreamcube.mazegame.server.control

import de.dreamcube.mazegame.server.config.BaitGeneratorDto
import de.dreamcube.mazegame.server.config.ConnectionDto
import de.dreamcube.mazegame.server.config.GeneratorParametersDto


data class ServerInformationDto(val connection: ConnectionDto, val mazeInformation: MazeInformationDto, val gameInformation: GameInformationDto)
data class MazeInformationDto(val generatorMode: String, val generatorParameters: GeneratorParametersDto, val walkableFields: Int)
data class GameInformationDto(
    val speed: String,
    val autoTrapeater: Boolean,
    val allowSpectator: Boolean,
    val delayCompensation: Boolean,
    val baitInformation: BaitInformationDto,
    val activePlayers: Int,
    val availableBotNames: List<String>
)

data class BaitInformationDto(
    val baitGenerator: BaitGeneratorDto,
    val baseBaitCount: Int,
    val desiredBaitCount: Int,
    val currentBaitCount: Int,
    val maxTrapCount: Int,
    val currentTrapCount: Int,
    val visibleTrapCount: Int,
)

data class PlayerInformationDto(
    val id: Int,
    val nick: String,
    val score: Int,
    val serverSided: Boolean,
    val delayOffset: Long,
    val totalPlayTime: PlayTimeDto,
    val currentPlayTime: PlayTimeDto,
    val currentPointsPerMinute: Int,
    val spectator: Boolean
)

data class PlayTimeDto(val milliseconds: Long, val time: String)
