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