package de.dreamcube.mazegame.common.maze

enum class BaitPositionChange(val shortName: String) {
    GENERATED("app"),
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