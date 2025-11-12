package de.dreamcube.mazegame.common.api

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Enum class for the game speed. The values contain a [delay], indicating how much time should pass between the last
 * received move and the next "RDY." command. The [shortName] is used in DTO classes.
 */
enum class GameSpeed(val delay: Long, @get:JsonValue val shortName: String) {
    UNLIMITED(1L, "unlimited"),
    ULTRA(50L, "ultra"),
    FAST(100L, "fast"),
    NORMAL(150L, "normal"),
    SLOW(200, "slow"),
    ULTRA_SLOW(300, "ultra-slow");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromShortName(shortName: String): GameSpeed? = when (shortName) {
            UNLIMITED.shortName -> UNLIMITED
            ULTRA.shortName -> ULTRA
            FAST.shortName -> FAST
            NORMAL.shortName -> NORMAL
            SLOW.shortName -> SLOW
            ULTRA_SLOW.shortName -> ULTRA_SLOW
            else -> null
        }
    }

    override fun toString(): String = "$shortName ($delay ms)"
}