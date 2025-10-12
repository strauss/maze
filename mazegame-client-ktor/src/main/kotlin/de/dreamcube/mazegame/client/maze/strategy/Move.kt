package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.common.maze.ViewDirection

enum class Move {
    /**
     * Don't move at all.
     */
    DO_NOTHING,

    /**
     * Turn left.
     */
    TURN_L,

    /**
     * Turn right.
     */
    TURN_R,

    /**
     * Step forward.
     */
    STEP;

    /**
     * Determines the new [ViewDirection], if this [Move] is applied, while being in [viewDirectionBefore].
     */
    fun getViewDirectionAfter(viewDirectionBefore: ViewDirection) {
        when (this) {
            DO_NOTHING, STEP -> viewDirectionBefore
            TURN_R -> viewDirectionBefore.turnRight()
            TURN_L -> viewDirectionBefore.turnLeft()
        }
    }
}