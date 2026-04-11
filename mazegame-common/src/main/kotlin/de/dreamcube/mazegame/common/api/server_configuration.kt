/*
 * Maze Game
 * Copyright (c) 2025-2026 Sascha Strauß
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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.dreamcube.mazegame.common.maze.DEFAULT_MAX_CLIENTS
import de.dreamcube.mazegame.common.maze.DEFAULT_RANDOM_HEIGHT
import de.dreamcube.mazegame.common.maze.DEFAULT_RANDOM_WIDTH

/**
 * Connection data.
 * - [port]: Network port. Should be set.
 * - [maxClients]: maximum number of clients, including spectators. Default is 20.
 * - [loginTimeout]: Timeout until the server "hangs up". Default is 30 seconds.
 * - [instantFlush]: If set, every message is flushed instantly (that's how the old server behaved). Default is false.
 */
data class ConnectionDto(
    val port: Int = 0,
    val maxClients: Int = DEFAULT_MAX_CLIENTS,
    val loginTimeout: Long = 30_000L,
    val instantFlush: Boolean = false
)

/**
 * Does the map file refer to a ressource or file. Determines how the file is tried to be read.
 */
enum class MapFileMode {
    /**
     * The maze file is read as a ressource. In order for this to work, the map file has to be in the classpath of the
     * application.
     */
    RESSOURCE,

    /**
     * The maze file is read as a file. Here, the file can be anywhere, but if you specify an indirect path, the working
     * directory of the application has to be taken into account.
     */
    FILE
}

/**
 * Controls the maze generator.
 * - [width]: Default 40; not relevant if in [GeneratorMode.MAP] or [GeneratorMode.TEMPLATE].
 * - [height]: Default 30; not relevant if in [GeneratorMode.MAP] or [GeneratorMode.TEMPLATE].
 * - [mapFile]: Points to a resource location of a map file. Can refer to a map or template.
 * - [mapFileMode]: Read map file as ressource or file?
 * - [templateFillStartPoints]: Only relevant if in [GeneratorMode.TEMPLATE]. If the template contains start positions
 * for walls ('?'), the flag indicates if further random start positions for walls should be generated or not. The
 * default is false. If the template file does not contain any starting positions for walls, this flag is not relevant.
 */
data class GeneratorParametersDto(
    val width: Int = DEFAULT_RANDOM_WIDTH,
    val height: Int = DEFAULT_RANDOM_HEIGHT,
    val mapFile: String? = null,
    val mapFileMode: MapFileMode = MapFileMode.RESSOURCE,
    val templateFillStartPoints: Boolean = false
)

/**
 * Configuration for the maze generator. See [GeneratorMode] for details.
 */
data class MazeGeneratorConfigurationDto(
    val generatorMode: GeneratorMode = GeneratorMode.RANDOM,
    val generatorParameters: GeneratorParametersDto = GeneratorParametersDto()
)

/**
 * Defines the nicknames of the "special" server-sided bots.
 * - [dummy]: the dummy bot. A movable obstacle for disturbing "good" strategies.
 * - [trapeater]: an important bot for removing traps. If no trapeater is defined, the traps can disrupt the whole map.
 * However, the number of traps is limited, so it is possible to go without.
 * - [frenzy]: a special bot that can perform various tasks to disturb the game flow. It is an optional "extra flavor"
 * for contests.
 * - [spectator]: Not a real bot. Clients that connect with this nickname can only watch and will never enter the maze.
 * This name is communicated to the clients when they request the metadata from the server. They can use it to
 * automagically name intended spectators correctly.
 */
data class SpecialBotsDto(
    val dummy: String = "dummy",
    val trapeater: String = "trapeater",
    val frenzy: String = "frenzy",
    val spectator: String = "spectator"
)

/**
 * Can be used to map arbitrary server-sided bots to different additional nicknames.
 */
data class FreeNickMapping(val botName: String, val nickNames: Set<String> = setOf())

/**
 * Contains nickname mappings for server-sided bots. The original bot name is always implicitly included.
 */
data class NickMappingsDto(
    val dummyNames: Set<String> = setOf(),
    val trapeaterNames: Set<String> = setOf(),
    val frenzyNames: Set<String> = setOf(),
    val freeNickMappings: List<FreeNickMapping> = listOf()
)

/**
 * Server bot configuration. [autoLaunch] contains a list of all bot names that should automatically spawn when the
 * server starts. [autoLaunchDelay] specifies the time a server waits for the client connection to be established. The
 * value is given in milliseconds and capped between 100 and 5000.
 */
data class ServerBotsDto(
    val autoLaunch: List<String> = listOf(),
    val autoLaunchDelay: Long = 1000L,
    val specialBots: SpecialBotsDto = SpecialBotsDto(),
    val nickMappings: NickMappingsDto = NickMappingsDto()
) {

    @JsonIgnore
    val actualAutoLaunchDelay: Long = autoLaunchDelay.coerceIn(100L..5000L)

    /**
     * Convenience attribute, containing all special bot names.
     */
    @JsonIgnore
    val specialBotNames: Set<String> = buildSet {
        add(specialBots.dummy)
        addAll(nickMappings.dummyNames)
        add(specialBots.trapeater)
        addAll(nickMappings.trapeaterNames)
        add(specialBots.frenzy)
        addAll(nickMappings.frenzyNames)
    }
}

/**
 * Allows some control on how baits are generated.
 * - The [objectDivisor] determines the maximum number of baits in relation to the number of walkable fields. The
 * default value is 26.
 * - The [trapDivisor] determines how many traps are allowed on the field. The default value is 4.
 * - The [invisibleGemProbability] determines, how often a gem spawns invisible. The default is 0.15 (15%)
 * - The [invisibleTrapProbability] determines, how often a trap spawns invisible. The default is 0.5 (50%)
 */
data class BaitGeneratorDto(
    val objectDivisor: Int = 26,
    val trapDivisor: Int = 4,
    val invisibleGemProbability: Double = 0.15,
    val invisibleTrapProbability: Double = 0.5
)

/**
 * With this, it is possible to control the random events. Only if [enabled], the events occur at all. The
 * [eventCooldown] is given in milliseconds. The default corresponds to 90 seconds. When an event occurs, at least that
 * amount of time will not contain further random events. The other values are probabilities (between 0 and 1):
 * - [allTrapProbability]: Whenever a gem is collected, it could be a "blood diamond", causing all baits to become
 * traps. The default probability is 0.5%.
 * - [allFoodProbability]: Whenever a coffee is collected, it could be a "coffee from the office machine", causing all
 * baits to become food. The default probability is 0.5%.
 * - [allCoffeeProbability]: Whenever a player collision happens, there is a chance, that all baits transform into
 * coffee, because the causing player was "too tired" to pay attention. The default probability is 0.5%.
 * - [allGemProbability]: Whenever a food is collected, it could be an "enchanted golden apple" causing all baits to
 * be transformed into gems. The default probability is 0.1%.
 * - [baitRushProbability]: Whenever an invisible trap is stepped on, there is a chance that a bait rush happens. The
 * default probability is 2,5%.
 * - [loseBaitProbability]: Whenever a player collision happens, there is also a chance that the causing player loses a
 * random bait at the collision location (and the corresponding points).
 */
data class SpecialEventsDto(
    val enabled: Boolean = false,
    val eventCooldown: Long = 90_000,
    val allTrapProbability: Double = 0.005,
    val allFoodProbability: Double = 0.005,
    val allCoffeeProbability: Double = 0.005,
    val allGemProbability: Double = 0.001,
    val baitRushProbability: Double = 0.025,
    val loseBaitProbability: Double = 0.20
)

/**
 * Contains the whole game configuration. The [initialSpeed] can be determined here. The default is [GameSpeed.NORMAL].
 * It can be specified whether baits should be generated right away with [generateBaitsAtStart]. The automatic trapeater
 * will only spawn if [autoTrapeater] is set. If [allowSpectator] is not set, the spectator mode is completely off.
 * The [delayCompensation] feature can also be configured here.
 *
 * The following two are no special game events. They are tightly bound to invisible gems and invisible traps.
 * - [uncoverInvisibleBaitsOnInvisibleGemProbability]: probability if all invisible baits should be uncovered if an
 * invisible gem is collected. The default value is 50%.
 * - [disarmInvisibleTrapProbability]: probability of bots being able to disarm invisible traps. They then either
 * destroy them or place them behind themselves (this is always a 50:50 coin flip). The default value is 30%.
 *
 * For the other two configurations see [BaitGeneratorDto] and [SpecialEventsDto].
 */
data class GameDto(
    val initialSpeed: GameSpeed = GameSpeed.NORMAL,
    val generateBaitsAtStart: Boolean = true,
    val autoTrapeater: Boolean = true,
    val allowSpectator: Boolean = true,
    val delayCompensation: Boolean = true,
    val uncoverInvisibleBaitsOnInvisibleGemProbability: Double = 0.25,
    val disarmInvisibleTrapProbability: Double = 0.3,
    val baitGenerator: BaitGeneratorDto = BaitGeneratorDto(),
    val events: SpecialEventsDto = SpecialEventsDto()
)

/**
 * Contains the whole server configuration containing everything mentioned in the corresponding classes.
 */
class MazeServerConfigurationDto(
    val connection: ConnectionDto = ConnectionDto(),
    val maze: MazeGeneratorConfigurationDto = MazeGeneratorConfigurationDto(),
    val serverBots: ServerBotsDto = ServerBotsDto(),
    val game: GameDto = GameDto()
)