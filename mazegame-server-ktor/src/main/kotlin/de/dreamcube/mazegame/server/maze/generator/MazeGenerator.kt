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

package de.dreamcube.mazegame.server.maze.generator

import de.dreamcube.mazegame.common.api.GeneratorMode
import de.dreamcube.mazegame.common.api.GeneratorParametersDto
import de.dreamcube.mazegame.common.api.MapFileMode
import de.dreamcube.mazegame.common.api.MazeGeneratorConfigurationDto
import de.dreamcube.mazegame.common.maze.MAX_RANDOM_HEIGHT
import de.dreamcube.mazegame.common.maze.MAX_RANDOM_WITH
import de.dreamcube.mazegame.common.maze.MIN_RANDOM_HEIGHT
import de.dreamcube.mazegame.common.maze.MIN_RANDOM_WIDTH
import de.dreamcube.mazegame.server.maze.Maze
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.math.max
import kotlin.math.min

private val LOGGER: Logger = LoggerFactory.getLogger(MazeGenerator::class.java)

interface MazeGenerator {

    /**
     * Generates a random maze with the given size defined by [width] and [height]
     */
    fun generateMaze(width: Int, height: Int): Maze

    /**
     * Generates a random maze. The given [maze] is expected to be a predefined map where the borders are already present. This variant is useful if
     * non-rectangular maps are desired. The generation happens "in-place".
     */
    fun generateMaze(maze: Maze): Maze
}

fun MazeGenerator.generateMazeFromConfiguration(configuration: MazeGeneratorConfigurationDto): Maze {
    val generatorMode: GeneratorMode = configuration.generatorMode
    val generatorParameters: GeneratorParametersDto = configuration.generatorParameters

    return when (generatorMode) {
        GeneratorMode.RANDOM -> {
            createRandom(generatorParameters)
        }

        GeneratorMode.TEMPLATE -> {
            val maze = generateMaze(readMapOrRandom(generatorParameters))
            checkDimensions(maze)
            maze
        }

        GeneratorMode.MAP -> {
            val maze = readMapOrRandom(generatorParameters)
            checkDimensions(maze)
            maze
        }
    }
}

private fun checkDimensions(maze: Maze) {
    if (maze.width < MIN_RANDOM_WIDTH || maze.width > MAX_RANDOM_WITH || maze.height < MIN_RANDOM_HEIGHT || maze.height > MAX_RANDOM_HEIGHT) {
        LOGGER.warn("Maze dimensions are outside of recommended dimensions.")
    }
}

private fun MazeGenerator.readMapOrRandom(generatorParameters: GeneratorParametersDto): Maze {
    val resourcePath: String? = generatorParameters.mapFile
    return if (resourcePath == null) {
        LOGGER.warn("No resource path configured. Falling back to random generation.")
        createRandom(generatorParameters)
    } else {
        val map: Maze = when (generatorParameters.mapFileMode) {
            MapFileMode.RESSOURCE -> parseMazeLinesFromResource(resourcePath)
            MapFileMode.FILE -> parseMazeLinesFromFile(resourcePath)
        }
        if (map.height == 0 || map.width == 0) {
            LOGGER.warn("Empty map file or map file not found. Falling back to random generation.")
            createRandom(generatorParameters)
        } else {
            map
        }
    }
}

private fun MazeGenerator.createRandom(generatorParameters: GeneratorParametersDto): Maze {
    val width = max(MIN_RANDOM_WIDTH, min(MAX_RANDOM_WITH, generatorParameters.width))
    val height = max(MIN_RANDOM_HEIGHT, min(MAX_RANDOM_HEIGHT, generatorParameters.height))
    return generateMaze(width, height)
}

private fun parseMazeLinesFromResource(resource: String): Maze {
    val mazeLines: List<String> =
        object {}.javaClass.getResourceAsStream(resource)?.bufferedReader(Charsets.UTF_8)?.readLines() ?: emptyList()
    return Maze.fromLines(mazeLines)
}

private fun parseMazeLinesFromFile(filename: String): Maze {
    val path = Path(filename)
    LOGGER.info("Trying to read map from '${path.absolutePathString()}'")
    return if (path.exists()) {
        val mazeLines: List<String> = path.readText().trim().lines()
        Maze.fromLines(mazeLines)
    } else Maze(0, 0)
}