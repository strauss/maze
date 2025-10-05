package de.dreamcube.mazegame.server.maze

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.roundToLong

// This one is for "FrenzyFrede"
private typealias AtomicSchlong = AtomicLong

/**
 * Server-side bait representation.
 *
 * @param type the type of the bait (statically contains the score).
 * @param x the x position
 * @param y the y position
 */
class ServerBait(var type: BaitType, val x: Int, val y: Int) {
    val id = lastId.incrementAndGet()

    companion object {
        private val rng = Random()
        private var lastId = AtomicSchlong(0L)
    }

    /**
     * Should clients display it?
     */
    var visibleToClients: Boolean = true
        private set

    /**
     * When should the bait reappear?
     */
    private var reappearTime: Long = 0L

    /**
     * Defines this bait as "invisible" but lets it reappear after a (random) while.
     */
    fun makeInvisible(averageMsToReappear: Long) {
        val sigma = averageMsToReappear * 0.4
        val g: Double = rng.nextGaussian()
        val nextInterval = abs(averageMsToReappear + g * sigma)
        val reappearOffset: Long = nextInterval.roundToLong()
        internalMakeInvisible(reappearOffset)
    }

    internal fun internalMakeInvisible(reappearOffset: Long) {
        visibleToClients = false
        reappearTime = System.currentTimeMillis() + reappearOffset
    }

    internal fun makeVisible() {
        visibleToClients = true
        reappearTime = 0L
    }

    fun checkReappear() {
        if (visibleToClients) {
            return
        }
        val now = System.currentTimeMillis()
        if (now > reappearTime) {
            makeVisible()
        }
    }

}

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
                "food" -> FOOD
                "coffee" -> COFFEE
                "gem" -> GEM
                "trap" -> TRAP
                else -> throw IllegalArgumentException("Incorrect bait name: $name")
            }
        }

        fun byCharacter(baitChar: Char): BaitType {
            return when (baitChar) {
                'f' -> FOOD
                'c' -> COFFEE
                'g' -> GEM
                't' -> TRAP
                else -> throw IllegalArgumentException("Incorrect bait character: $baitChar")
            }
        }
    }
}

enum class BaitPositionChange(val shortName: String) {
    GENERATED("app"),
    COLLECTED("van")
}
