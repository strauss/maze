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
