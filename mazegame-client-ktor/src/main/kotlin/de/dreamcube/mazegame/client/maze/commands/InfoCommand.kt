package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.InfoCode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InfoCommand(mazeClient: MazeClient, commandWithParameters: List<String>) : ClientSideCommand(mazeClient) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MazeClient::class.java)
    }

    private val infoNumber: Int
    private val infoCode: InfoCode
    private val message: String?
    private val sourcePlayerId: Int?
    private val gameSpeed: Int?
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            infoNumber = 999
            infoCode = InfoCode.COMPLETELY_UNKNOWN
            message = null
            sourcePlayerId = null
            okay = false
            gameSpeed = null
        } else {
            infoNumber = commandWithParameters[1].toInt()
            infoCode = InfoCode.fromCode(infoNumber)
            if (commandWithParameters.size > 2) {
                if (infoCode == InfoCode.SPEED_CHANGE) {
                    message = null
                    gameSpeed = commandWithParameters[2].toInt()
                } else {
                    message = commandWithParameters[2]
                    gameSpeed = null
                }
            } else {
                message = null
                gameSpeed = null
            }
            sourcePlayerId = if (commandWithParameters.size > 3) commandWithParameters[3].toInt() else null
            okay = true
        }
    }

    override suspend fun internalExecute() {
        when (infoCode) {
            InfoCode.SERVER_MESSAGE -> {
                if (message != null) {
                    mazeClient.eventHandler.fireServerInfo(message)
                } else {
                    LOGGER.warn("Received empty server message.")
                }
            }

            InfoCode.CLIENT_MESSAGE, InfoCode.CLIENT_WHISPER -> {
                val sourcePlayerNick: String = getSourcePlayerNick()
                if (sourcePlayerId != null && message != null) {
                    mazeClient.eventHandler.firePlayerChat(
                        sourcePlayerId,
                        sourcePlayerNick,
                        message,
                        infoCode == InfoCode.CLIENT_WHISPER
                    )
                } else {
                    LOGGER.warn("Received invalid client message (something is null) with source player id '$sourcePlayerId', nick '$sourcePlayerNick' and message '$message'.")
                }
            }

            InfoCode.SPEED_CHANGE -> {
                if (gameSpeed != null) {
                    val oldSpeed: Int = mazeClient.gameSpeed
                    mazeClient.gameSpeed = gameSpeed
                    if (oldSpeed != gameSpeed) {
                        mazeClient.eventHandler.fireSpeedChanged(oldSpeed, gameSpeed)
                    }
                }
            }

            InfoCode.OK -> {
                // just ignore it
            }

            else -> {
                mazeClient.eventHandler.fireServerError(infoCode)
            }
        }
    }

    private suspend fun getSourcePlayerNick(): String {
        val sourcePlayerNick: String = sourcePlayerId?.let {
            mazeClient.players.getPlayerSnapshot(it)?.nick
        } ?: "<${sourcePlayerId ?: "unknown"}>"
        return sourcePlayerNick
    }
}