package de.dreamcube.mazegame.common.api

/**
 * Contains the full server information for one server.
 */
data class ServerInformationDto(
    val connection: ConnectionDto,
    val mazeInformation: MazeInformationDto,
    val gameInformation: GameInformationDto
)

/**
 * Contains the maze information and how it was generated.
 */
data class MazeInformationDto(
    val generatorMode: String,
    val generatorParameters: GeneratorParametersDto,
    val walkableFields: Int
)

/**
 * Contains the whole game information, basically reflecting parts of the configuration.
 */
data class GameInformationDto(
    val speed: String,
    val autoTrapeater: Boolean,
    val allowSpectator: Boolean,
    val delayCompensation: Boolean,
    val baitInformation: BaitInformationDto,
    val activePlayers: Int,
    val availableBotNames: List<String>
)

/**
 * Contains information about the current bait distribution and how they are generated.
 */
data class BaitInformationDto(
    val baitGenerator: BaitGeneratorDto,
    val baseBaitCount: Int,
    val desiredBaitCount: Int,
    val currentBaitCount: Int,
    val maxTrapCount: Int,
    val currentTrapCount: Int,
    val visibleTrapCount: Int,
)

/**
 * Contains the information of one player.
 */
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

/**
 * Contains the playtime in milliseconds and in human readable form.
 */
data class PlayTimeDto(val milliseconds: Long, val time: String)

/**
 * Contains a reduced sub set of player information. It is used for the openly available meta information that can be
 * queried without auth before the connection is established.
 */
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