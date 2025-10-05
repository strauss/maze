package de.dreamcube.mazegame.server.maze.generator

import de.dreamcube.mazegame.server.config.GeneratorParametersDto
import de.dreamcube.mazegame.server.config.MazeGeneratorConfigurationDto
import de.dreamcube.mazegame.server.maze.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        val map: Maze = parseMazeLinesFromResource(resourcePath)
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
    val mazeLines: List<String> = object {}.javaClass.getResourceAsStream(resource)?.bufferedReader(Charsets.UTF_8)?.readLines() ?: emptyList()
    return Maze.fromLines(mazeLines)
}