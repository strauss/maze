package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.*
import de.dreamcube.mazegame.server.maze.*
import de.dreamcube.mazegame.server.maze.game_events.BaitCollectedEvent
import de.dreamcube.mazegame.server.maze.game_events.GameEvent
import de.dreamcube.mazegame.server.maze.game_events.PlayerCollisionEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StepCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(StepCommand::class.java)
    }

    init {
        if (commandWithParameters.size != 1) {
            errorCode = InfoCode.WRONG_PARAMETER_VALUE
        } else @Suppress("kotlin:S6518") // WTF? you serious?
        if (!clientConnection.isReady.get()) {
            errorCode = InfoCode.ACTION_WITHOUT_READY
        }
        checkLoggedIn()
    }

    override suspend fun internalExecute() {
        val movementAllowed = clientConnection.unready()
        if (!movementAllowed) {
            return
        }
        val messagesForAll: MutableList<Message> = ArrayList()
        var gameEvent: GameEvent? = null
        val player: Player = clientConnection.player
        if (!mazeServer.maze.isWalkable(player.x, player.y, player.viewDirection)) {
            errorCode = InfoCode.WALL_CRASH
            return
        }
        var nX: Int = player.x
        var nY: Int = player.y
        val dir: ViewDirection = player.viewDirection
        when (dir) {
            ViewDirection.NORTH -> nY -= 1
            ViewDirection.EAST -> nX += 1
            ViewDirection.SOUTH -> nY += 1
            ViewDirection.WEST -> nX -= 1
        }

        if (mazeServer.maze.isOccupied(nX, nY)) {
            val bait: ServerBait? = mazeServer.getBaitAt(nX, nY)
            if (bait != null) {
                // collect bait
                gameEvent = BaitCollectedEvent(bait, player)
                val removeMessage: Message? = mazeServer.removeBait(bait)
                if (removeMessage != null) {
                    messagesForAll.add(removeMessage.thereIsMore())
                }
                player.score += bait.type.score
                messagesForAll.add(createPlayerScoreChangedMessage(player).thereIsMore())
                if (!bait.visibleToClients && bait.type != BaitType.TRAP) {
                    messagesForAll.add(createServerInfoMessage("${player.nick} found an invisible ${bait.type.baitName}.").thereIsMore())
                }
                if (bait.type == BaitType.TRAP) {
                    messagesForAll.add(mazeServer.teleportPlayerRandomly(player).thereIsMore())
                } else {
                    // step forward
                    mazeServer.changePlayerPosition(player, nX, nY, dir)
                    player.incrementMoveCounter()
                    messagesForAll.add(createPlayerPositionStepMessage(player).thereIsMore())
                }
                // replace consumed bait
                val newBaitMessages: List<Message> = mazeServer.replaceBaits()
                if (newBaitMessages.isNotEmpty()) {
                    messagesForAll.addAll(newBaitMessages)
                }
            } else {
                val otherPlayer: Player? = mazeServer.getPlayerAt(nX, nY)
                if (otherPlayer != null) {
                    // teleportation on collision
                    gameEvent = PlayerCollisionEvent(player, otherPlayer, nX, nY)
                    messagesForAll.add(mazeServer.teleportPlayerRandomly(player, player.id).thereIsMore())
                    messagesForAll.add(mazeServer.teleportPlayerRandomly(otherPlayer, player.id).thereIsMore())
                } else {
                    LOGGER.warn("Cell was marked as 'occupied', but it wasn't: ($nX : $nY). Will perform step command anyway.")
                    // step forward
                    mazeServer.changePlayerPosition(player, nX, nY, dir)
                    player.incrementMoveCounter()
                    messagesForAll.add(createPlayerPositionStepMessage(player).thereIsMore())
                }
            }
        } else {
            // step forward
            mazeServer.changePlayerPosition(player, nX, nY, dir)
            player.incrementMoveCounter()
            messagesForAll.add(createPlayerPositionStepMessage(player).thereIsMore())
        }
        if (messagesForAll.isNotEmpty()) {
            messagesForAll.add(createEmptyLastMessage())
        }
        mazeServer.sendToAllPlayers(*messagesForAll.toTypedArray())
        if (mazeServer.gameEventsEnabled && gameEvent != null) {
            mazeServer.gameEventControl.addEvent(gameEvent)
        }
    }
}