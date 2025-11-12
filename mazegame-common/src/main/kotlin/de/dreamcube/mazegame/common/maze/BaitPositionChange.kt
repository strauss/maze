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