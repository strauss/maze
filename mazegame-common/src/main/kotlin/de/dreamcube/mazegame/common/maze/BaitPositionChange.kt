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

package de.dreamcube.mazegame.common.maze

/**
 * Indicates, whether a bait appears or vanishes.
 */
enum class BaitPositionChange(val shortName: String) {
    /**
     * A new bait was generated and therefore appears now.
     */
    GENERATED("app"),

    /**
     * An existing bait was collected and therefore vanishes now.
     */
    COLLECTED("van");

    companion object {
        @JvmStatic
        fun byName(name: String): BaitPositionChange {
            return when (name) {
                GENERATED.shortName -> GENERATED
                COLLECTED.shortName -> COLLECTED
                else -> throw IllegalArgumentException("Incorrect bait position change name: $name")
            }
        }
    }
}