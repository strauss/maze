package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.BaitEventListener
import de.dreamcube.mazegame.client.maze.events.ClientConnectionStatusListener
import de.dreamcube.mazegame.client.maze.events.MazeEventListener
import de.dreamcube.mazegame.client.maze.events.PlayerMovementListener
import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MazeModel() : MazeEventListener, BaitEventListener, PlayerMovementListener, ClientConnectionStatusListener {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MazeModel::class.java)
    }

    enum class PathOccupationStatus {
        EMPTY, PLAYER, BAIT, CROWDED
    }

    sealed interface MazeField
    object UnknownMazeField : MazeField
    object OutsideMazeField : MazeField
    object WallMazeField : MazeField

    class PathMazeField() : MazeField {
        var bait: BaitType? = null
            internal set
        var player: PlayerSnapshot? = null
            internal set
        val occupationStatus
            get() =
                when {
                    bait == null && player == null -> PathOccupationStatus.EMPTY
                    bait != null && player == null -> PathOccupationStatus.BAIT
                    bait == null && player != null -> PathOccupationStatus.PLAYER
                    else -> PathOccupationStatus.CROWDED // this should not happen, but you never know
                }
    }

    init {
        UiController.prepareEventListener(this)
    }

    internal var width: Int = 0
        private set
    internal var height: Int = 0
        private set
    var internalMaze: Array<MazeField> = Array(0) { UnknownMazeField }
    var mazeReceived: Boolean = false
        private set

    private fun reset() {
        width = 0
        height = 0
        internalMaze = Array(0) { UnknownMazeField }
        mazeReceived = false
    }

    override fun onMazeReceived(width: Int, height: Int, mazeLines: List<String>) {
        this.width = width
        this.height = height
        internalMaze = Array(width * height) { UnknownMazeField }
        var y = 0
        for (currentLine: String in mazeLines) {
            if (currentLine.length != width) {
                LOGGER.error("Maze line should have length of $width but has length ${currentLine.length}.")
            }
            var x = 0
            for (c: Char in currentLine) {
                this[x, y] = when (c) {
                    '-' -> OutsideMazeField
                    '#' -> WallMazeField
                    '.' -> PathMazeField()
                    else -> UnknownMazeField
                }
                x += 1
            }
            y += 1
        }
        if (y != height) {
            LOGGER.error("Received $y maze lines but expected $height. Some fields will be shown as 'UNKNOWN'.")
        }
        mazeReceived = true
    }

    private operator fun set(x: Int, y: Int, value: MazeField) {
        val position: Int = y * width + x
        internalMaze[position] = value
    }

    internal operator fun get(x: Int, y: Int): MazeField {
        val position: Int = y * width + x
        return internalMaze[position]
    }

    override fun onBaitAppeared(bait: Bait) {
        val mazeField = this[bait.x, bait.y]
        if (mazeField is PathMazeField) {
            mazeField.bait = bait.type
            UiController.triggerMazeUpdate(bait.x, bait.y)
        }
    }

    override fun onBaitVanished(bait: Bait) {
        val mazeField = this[bait.x, bait.y]
        if (mazeField is PathMazeField) {
            mazeField.bait = null
            UiController.triggerMazeUpdate(bait.x, bait.y)
        }
    }

    override fun onPlayerAppear(playerSnapshot: PlayerSnapshot) {
        updatePlayerAtField(playerSnapshot)
    }

    override fun onPlayerTurn(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        if (oldPosition.x == newPlayerSnapshot.x && oldPosition.y == newPlayerSnapshot.y) {
            updatePlayerAtField(newPlayerSnapshot)
        } else {
            LOGGER.warn("Turn command with position change.")
            movePlayer(oldPosition, newPlayerSnapshot)
        }
    }

    private fun updatePlayerAtField(playerSnapshot: PlayerSnapshot) {
        val mazeField = this[playerSnapshot.x, playerSnapshot.y]
        if (mazeField is PathMazeField) {
            mazeField.player = playerSnapshot
            if (mazeField.occupationStatus == PathOccupationStatus.PLAYER) {
                UiController.triggerMazeUpdate(playerSnapshot.x, playerSnapshot.y)
            }
        }
    }

    override fun onPlayerVanish(playerSnapshot: PlayerSnapshot) {
        val mazeField = this[playerSnapshot.x, playerSnapshot.y]
        if (mazeField is PathMazeField) {
            mazeField.player = null
            UiController.triggerMazeUpdate(playerSnapshot.x, playerSnapshot.y)
        }
    }

    override fun onPlayerStep(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        movePlayer(oldPosition, newPlayerSnapshot)
    }

    override fun onPlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        movePlayer(oldPosition, newPlayerSnapshot)
    }

    private fun movePlayer(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot
    ) {
        val previousMazeField = this[oldPosition.x, oldPosition.y]
        if (previousMazeField is PathMazeField) {
            previousMazeField.player = null
            UiController.triggerMazeUpdate(oldPosition.x, oldPosition.y)
        }
        val mazeField = this[newPlayerSnapshot.x, newPlayerSnapshot.y]
        if (mazeField is PathMazeField) {
            mazeField.player = newPlayerSnapshot
            UiController.triggerMazeUpdate(newPlayerSnapshot.x, newPlayerSnapshot.y)

            // If we have a marked player, we write their maze coordinates to the status bar
            val playerToMark: UiPlayerInformation? = UiController.markerPane.playerToMark
            if (playerToMark != null && playerToMark.id == newPlayerSnapshot.id) {
                UiController.uiScope.launch {
                    UiController.updatePositionStatus(newPlayerSnapshot.x, newPlayerSnapshot.y)
                }
            }
        }
    }

    override fun onConnectionStatusChange(
        oldStatus: ConnectionStatus,
        newStatus: ConnectionStatus
    ) {
        if (newStatus == ConnectionStatus.DEAD) {
            reset()
        }
    }
}