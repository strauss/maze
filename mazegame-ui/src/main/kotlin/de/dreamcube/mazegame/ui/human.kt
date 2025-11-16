package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.events.BaitEventListener
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.strategy.Bot
import de.dreamcube.mazegame.client.maze.strategy.Move
import de.dreamcube.mazegame.client.maze.strategy.SingleTargetAStar
import de.dreamcube.mazegame.client.maze.strategy.Strategy
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.ViewDirection
import kotlinx.coroutines.runBlocking
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

@Suppress("unused")
@Bot("spectator", isSpectator = true, flavor = "Who will win? Nobody knows!")
class Spectator : Strategy() {

    /**
     * A spectator does ... nothing
     */
    override fun getNextMove(): Move = Move.DO_NOTHING

}

@Suppress("unused")
@Bot("humanFirstPerson", isHuman = true, flavor = "User arrow keys: left/right for turn and up for step")
class HumanPlayerFirstPerson : Strategy(), ClientConnectionStatusListener {

    private val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    private val keyEventDispatcher = KeyEventDispatcher { e ->
        if (e != null && e.id == KeyEvent.KEY_PRESSED) {
            when (e.keyCode) {
                KeyEvent.VK_UP -> nextMove = Move.STEP
                KeyEvent.VK_LEFT -> nextMove = Move.TURN_L
                KeyEvent.VK_RIGHT -> nextMove = Move.TURN_R
            }
        }
        false
    }

    private var nextMove = Move.DO_NOTHING

    override fun getNextMove(): Move {
        val move = nextMove
        nextMove = Move.DO_NOTHING
        return move
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        when (newStatus) {
            ConnectionStatus.PLAYING -> {
                kfm.addKeyEventDispatcher(keyEventDispatcher)
            }

            ConnectionStatus.DEAD -> {
                kfm.removeKeyEventDispatcher(keyEventDispatcher)
            }

            else -> {
                // do nothing
            }
        }
    }
}

@Suppress("unused")
@Bot("humanThirdPerson", isHuman = true, flavor = "Use all four arrow keys")
class HumanPlayerThirdPerson : Strategy(), ClientConnectionStatusListener {

    private val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    private val keyEventDispatcher = KeyEventDispatcher { e ->
        if (e != null && e.id == KeyEvent.KEY_PRESSED) {
            val viewDirection = this.viewDirection
            when (e.keyCode) {
                KeyEvent.VK_UP -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.NORTH -> Move.STEP
                        ViewDirection.WEST -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }

                KeyEvent.VK_LEFT -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.WEST -> Move.STEP
                        ViewDirection.SOUTH -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }

                KeyEvent.VK_RIGHT -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.EAST -> Move.STEP
                        ViewDirection.NORTH -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }

                KeyEvent.VK_DOWN -> {
                    nextMove = when (viewDirection) {
                        ViewDirection.SOUTH -> Move.STEP
                        ViewDirection.EAST -> Move.TURN_R
                        else -> Move.TURN_L
                    }
                }
            }
        }
        false
    }

    private var nextMove = Move.DO_NOTHING

    private val viewDirection: ViewDirection
        get() = mazeClient.ownPlayerSnapshot.viewDirection

    override fun getNextMove(): Move {
        val move = nextMove
        nextMove = Move.DO_NOTHING
        return move
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        when (newStatus) {
            ConnectionStatus.PLAYING -> {
                kfm.addKeyEventDispatcher(keyEventDispatcher)
            }

            ConnectionStatus.DEAD -> {
                kfm.removeKeyEventDispatcher(keyEventDispatcher)
            }

            else -> {
                // do nothing
            }
        }
    }
}

@Suppress("unused")
@Bot("clickAndCollect", isHuman = true, flavor = "Click on bait, I will collect it!")
class HumanPlayerLazy : SingleTargetAStar(), ClientConnectionStatusListener, MazeCellListener,
    BaitEventListener {

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        when (newStatus) {
            ConnectionStatus.PLAYING -> {
                UiController.addMazeCellListener(this)
                UiController.activateHoverMarks()
            }

            ConnectionStatus.DEAD -> {
                UiController.removeMazeCellListener(this)
                UiController.deactivateHoverMarks()
            }

            else -> {
                // do nothing
            }
        }
    }

    override fun onMazeCellSelected(x: Int, y: Int, mazeField: MazeModel.MazeField) {
        if (mazeField is MazeModel.PathMazeField && mazeField.occupationStatus == MazeModel.PathOccupationStatus.BAIT) {
            val bait: Bait? = runBlocking { mazeClient.getBaitAt(x, y) }
            bait?.let {
                currentTarget = it
                path.clear()
            }
        }
    }

    override fun onBaitAppeared(bait: Bait) {
        // ignore
    }

    override fun onBaitVanished(bait: Bait) {
        if (currentTarget == bait) {
            currentTarget = null
            path.clear()
        }
    }

}