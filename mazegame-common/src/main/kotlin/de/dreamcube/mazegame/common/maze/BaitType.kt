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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kotlin.math.abs

/**
 * Contains information which bait types exist, how they are called and their score.
 */
enum class BaitType(
    @get:JsonValue
    val baitName: String,
    val score: Int
) {
    FOOD("food", 13),
    COFFEE("coffee", 42),
    GEM("gem", 314),
    TRAP("trap", -128);

    companion object {
        @JvmStatic
        @JsonCreator
        fun byName(name: String): BaitType {
            return when (name) {
                FOOD.baitName -> FOOD
                COFFEE.baitName -> COFFEE
                GEM.baitName -> GEM
                TRAP.baitName -> TRAP
                else -> throw IllegalArgumentException("Incorrect bait name: $name")
            }
        }

        fun byCharacter(baitChar: Char): BaitType {
            return when (baitChar.lowercaseChar()) {
                'f' -> FOOD
                'c' -> COFFEE
                'g' -> GEM
                't' -> TRAP
                else -> throw IllegalArgumentException("Incorrect bait character: $baitChar")
            }
        }

        fun byScore(score: Int): BaitType? {
            return when (abs(score)) {
                FOOD.score -> FOOD
                COFFEE.score -> COFFEE
                GEM.score -> GEM
                -TRAP.score -> TRAP
                else -> null
            }
        }
    }

    val asChar: Char
        get() = baitName.first().uppercaseChar()

    override fun toString(): String = baitName
}