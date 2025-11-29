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

package de.dreamcube.mazegame.server.maze

import de.dreamcube.mazegame.common.maze.BaitType
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

