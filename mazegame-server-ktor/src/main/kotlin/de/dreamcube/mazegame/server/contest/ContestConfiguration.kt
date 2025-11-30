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

package de.dreamcube.mazegame.server.contest

import de.dreamcube.mazegame.common.api.GameSpeed

enum class ContestEventType {
    /**
     * Starts the contest. Should occur only once. Having it multiple times just results in error logs, but nothing bad
     * will happen.
     */
    START,

    /**
     * Give a status report with the top n players.
     */
    REPORT,

    /**
     * Spawns the frenzy bot if the configured one is available.
     */
    SPAWN_FRENZY,

    /**
     * Despawns the frenzy bot if it is running.
     */
    DESPAWN_FRENZY,

    /**
     * Increases the game speed.
     */
    SPEED_UP,

    /**
     * Decreases the game speed.
     */
    SLOW_DOWN,

    /**
     * Transforms all baits into traps.
     */
    ALL_TRAPS,

    /**
     * Transforms all baits into food.
     */
    ALL_FOOD,

    /**
     * Transforms all baits into coffee.
     */
    ALL_COFFEE,

    /**
     * Transforms all baits into gems.
     */
    ALL_GEMS,

    /**
     * Triggers a bait rush.
     */
    BAIT_RUSH,

    /**
     * Removes all baits and generates new ones.
     */
    RE_BAIT,

    /**
     * All players will be teleported randomly.
     */
    SHUFFLE_PLAYERS,

    /**
     * Ends the contest and reports the winner. Should occur only once.
     */
    STOP
}

enum class CuratedEventSet {
    /**
     * Separates the contest into three thirds. The second third will contain a frenzy bot.
     */
    DEFAULT_FRENZY_MODE,

    /**
     * Separates the contest into three thirds. The game speed will rise at the beginning of every third.
     */
    SPEED_UP,

    /**
     * Separates the contest into three thirds. In the middle of every third there will be a brief bait rush.
     */
    BAIT_RUSH,

    /**
     * Separates the contest into three thirds. At the end of the first third, the players are shuffled. At the end
     * of the second third, the baits are regenerated.
     */
    SHUFFLE_AND_REBAIT;

    internal fun toEventList(durationInMinutes: Double): List<ContestEvent> = when (this) {
        DEFAULT_FRENZY_MODE -> defaultFrenzyEventList(durationInMinutes)
        SPEED_UP -> speedUpEventList(durationInMinutes)
        BAIT_RUSH -> baitRushEventList(durationInMinutes)
        SHUFFLE_AND_REBAIT -> shuffleAndReBait(durationInMinutes)
    }
}

data class ContestEvent(val type: ContestEventType, val delayInMinutes: Double)

private fun defaultFrenzyEventList(durationInMinutes: Double): List<ContestEvent> {
    val thirdDurationInMinutes = durationInMinutes / 3.0
    return listOf(
        // after the first third
        ContestEvent(ContestEventType.SPAWN_FRENZY, thirdDurationInMinutes),
        // after the second third
        ContestEvent(ContestEventType.DESPAWN_FRENZY, thirdDurationInMinutes * 2.0)
    )
}

private fun speedUpEventList(durationInMinutes: Double): List<ContestEvent> {
    val thirdDurationInMinutes = durationInMinutes / 3.0
    return listOf(
        // after the first third
        ContestEvent(ContestEventType.SPEED_UP, thirdDurationInMinutes * 0.99),
        // after the second third
        ContestEvent(ContestEventType.SPEED_UP, thirdDurationInMinutes * 1.99)
    )
}

private fun baitRushEventList(durationInMinutes: Double): List<ContestEvent> {
    val thirdDurationInMinutes = durationInMinutes / 3.0
    return listOf(
        // in the middle of the first third
        ContestEvent(ContestEventType.BAIT_RUSH, thirdDurationInMinutes * 0.5),
        // in the middle of the second third
        ContestEvent(ContestEventType.BAIT_RUSH, thirdDurationInMinutes * 1.5),
        // in the middle of the last third
        ContestEvent(ContestEventType.BAIT_RUSH, thirdDurationInMinutes * 2.5)
    )
}

private fun shuffleAndReBait(durationInMinutes: Double): List<ContestEvent> {
    val thirdDurationInMinutes = durationInMinutes / 3.0
    return listOf(
        // after the first third
        ContestEvent(ContestEventType.SHUFFLE_PLAYERS, thirdDurationInMinutes * 1.01),
        // after the second third
        ContestEvent(ContestEventType.RE_BAIT, thirdDurationInMinutes * 2.01)
    )
}

data class ContestConfiguration(
    val durationInMinutes: Double = 30.0,
    val statusReportIntervalInMinutes: Double = durationInMinutes / 6.0,
    val statusPositions: Int = 10,
    val initialGameSpeed: GameSpeed = GameSpeed.NORMAL,
    val eventSets: Set<CuratedEventSet> = setOf(CuratedEventSet.DEFAULT_FRENZY_MODE),
    val additionalEvents: List<ContestEvent> = listOf()
)
