package de.dreamcube.mazegame.common.api

data class ServerInformationDto(
    val connection: ConnectionDto,
    val mazeInformation: MazeInformationDto,
    val gameInformation: GameInformationDto
)

data class MazeInformationDto(
    val generatorMode: String,
    val generatorParameters: GeneratorParametersDto,
    val walkableFields: Int
)

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
    val currentPointsPerMinute: Double,
    val currentAvgMoveTimeInMs: Double,
    val spectator: Boolean
)

data class PlayTimeDto(val milliseconds: Long, val time: String)

data class ReducedServerInformationDto(
    val id: Int,
    val maxClients: Int,
    val activeClients: Int,
    val speed: Int,
    val width: Int,
    val height: Int,
    val compactMaze: String,
    val spectatorName: String? = null
) {
    override fun toString(): String = "$id ($width x $height)"
}