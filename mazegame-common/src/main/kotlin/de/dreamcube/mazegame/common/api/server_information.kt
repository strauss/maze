/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    val generatorMode: GeneratorMode,
    val generatorParameters: GeneratorParametersDto,
    val walkableFields: Int
)

/**
 * Contains the whole game information, basically reflecting parts of the configuration.
 */
data class GameInformationDto(
    val speed: GameSpeed,
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
 * Contains the playtime in milliseconds and in human-readable form.
 */
data class PlayTimeDto(val milliseconds: Long, val time: String)

/**
 * Contains a reduced sub set of server information. It is used for the openly available meta information that can be
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
    override fun toString(): String = if (id < 0) "Clear..." else "$id ($width x $height)"
}