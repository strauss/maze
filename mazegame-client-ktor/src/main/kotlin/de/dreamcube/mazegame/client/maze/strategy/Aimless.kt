package de.dreamcube.mazegame.client.maze.strategy

import de.dreamcube.mazegame.client.maze.events.ErrorInfoListener
import de.dreamcube.mazegame.common.maze.InfoCode
import kotlin.random.Random

@Bot("dummy")
@Suppress("unused")
class Aimless : Strategy(), ErrorInfoListener {

    private var nextMove: Move? = null
    private val rng = Random

    override fun getNextMove(): Move {
        botDelayInMs = rng.nextInt(mazeClient.gameSpeed)
        if (nextMove != null) {
            val moveToReturn: Move = nextMove!!
            nextMove = null
            return moveToReturn
        }
        val r = rng.nextDouble()
        return when {
            r < 0.05 -> Move.TURN_L
            r < 0.1 -> Move.TURN_R
            else -> Move.STEP
        }
    }

    override fun onServerError(infoCode: InfoCode) {
        if (infoCode == InfoCode.WALL_CRASH) {
            val r = rng.nextDouble()
            nextMove = if (r < 0.5) Move.TURN_L else Move.TURN_R
        }
    }

}