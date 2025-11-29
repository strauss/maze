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

enum class ContestEventType {
    START, REPORT, SPAWN_FRENZY, DESPAWN_FRENZY, STOP
}

data class ContestEvent(val type: ContestEventType, val delayInMinutes: Int)

private fun defaultAdditionalEventList(durationInMinutes: Int): List<ContestEvent> {
    val thirdDurationInMinutes = durationInMinutes / 3
    return listOf(
        // after first third
        ContestEvent(ContestEventType.SPAWN_FRENZY, thirdDurationInMinutes),
        // after second third
        ContestEvent(ContestEventType.DESPAWN_FRENZY, thirdDurationInMinutes * 2)
    )
}

data class ContestConfiguration(
    val durationInMinutes: Int = 30,
    val statusReportIntervalInMinutes: Int = 5,
    val statusPositions: Int = 10,
    val additionalEvents: List<ContestEvent> = defaultAdditionalEventList(durationInMinutes)
)
