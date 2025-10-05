package de.dreamcube.mazegame.server.maze.generator

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class GeneratorMode(@get:JsonValue val shortName: String) {
    /**
     * The maze is generated in a rectangle. The dimensions can be specified.
     */
    RANDOM("random"),

    /**
     * The maze is generated into a template map, given by a map file.
     */
    TEMPLATE("template"),

    /**
     * The map file is taken as it is, no further generation is performed on the given map file.
     */
    MAP("map");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromShortName(shortName: String): GeneratorMode? = when (shortName) {
            RANDOM.shortName -> RANDOM
            TEMPLATE.shortName -> TEMPLATE
            MAP.shortName -> MAP
            else -> null
        }
    }
}