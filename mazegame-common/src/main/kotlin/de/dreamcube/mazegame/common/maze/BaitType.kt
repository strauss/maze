package de.dreamcube.mazegame.common.maze

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kotlin.math.abs

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

}