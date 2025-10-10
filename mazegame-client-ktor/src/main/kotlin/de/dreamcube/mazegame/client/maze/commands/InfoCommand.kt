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
    override val okay: Boolean

    init {
        if (commandWithParameters.size < 2) {
            infoNumber = 999
            infoCode = InfoCode.COMPLETELY_UNKNOWN
            message = null
            sourcePlayerId = null
            okay = false
        } else {
            infoNumber = commandWithParameters[1].toInt()
            infoCode = InfoCode.fromCode(infoNumber)
            message = if (commandWithParameters.size > 2) commandWithParameters[2] else null
            sourcePlayerId = if (commandWithParameters.size > 3) commandWithParameters[3].toInt() else null
            okay = true
        }
    }

    override suspend fun internalExecute() {
        when (infoCode) {
            InfoCode.SERVER_MESSAGE -> {
                LOGGER.info("Server: $message")
                // TODO: fire server chat event
            }

            InfoCode.CLIENT_MESSAGE -> {
                val sourcePlayerNick: String = getSourcePlayerNick()
                LOGGER.info("$sourcePlayerNick: $message")
                // TODO: fire client chat event
            }

            InfoCode.CLIENT_WHISPER -> {
                val sourcePlayerNick: String = getSourcePlayerNick()
                LOGGER.info("$sourcePlayerNick(whisper): $message")
                // TODO: fire client whisper event
            }

            InfoCode.WRONG_PARAMETER_VALUE -> {
                LOGGER.warn("$infoNumber: Wrong parameter value")
                // TODO: fire warning event
            }

            InfoCode.TOO_MANY_CLIENTS -> {
                LOGGER.warn("$infoNumber: Server full (too many clients)")
                // TODO: fire warning event
            }

            InfoCode.DUPLICATE_NICK -> {
                LOGGER.warn("$infoNumber: Duplicate/Invalid nick")
                // TODO: fire warning event
            }

            InfoCode.WALL_CRASH -> {
                LOGGER.warn("$infoNumber: Tried to step into a wall")
                // TODO: fire warning event
            }

            InfoCode.ACTION_WITHOUT_READY -> {
                LOGGER.warn("$infoNumber: Did not wait for RDY.")
                // TODO: fire warning event
            }

            InfoCode.ALREADY_LOGGED_IN -> {
                LOGGER.warn("$infoNumber: Already logged in")
                // TODO: fire warning event
            }

            InfoCode.COMMAND_BEFORE_LOGIN -> {
                LOGGER.warn("$infoNumber: Not logged in")
                // TODO: fire warning event
            }

            InfoCode.LOGIN_TIMEOUT -> {
                LOGGER.error("$infoNumber: Login timed out")
                // TODO: fire error event
            }

            InfoCode.UNKNOWN_COMMAND -> {
                LOGGER.error("$infoNumber: The server did not understand our last command")
                // TODO: fire error event
            }

            InfoCode.PARAMETER_COUNT_INCORRECT -> {
                LOGGER.error("$infoNumber: Incorrect number of parameters")
                // TODO: fire error event
            }

            InfoCode.COMPLETELY_UNKNOWN -> {
                LOGGER.error("$infoNumber: We do not know this error code")
                // TODO: fire error event
            }

            else -> {
                // nothing
            }
        }
    }

    private suspend fun getSourcePlayerNick(): String {
        val sourcePlayerNick: String = sourcePlayerId?.let {
            mazeClient.players.getPlayerSnapshot(it)?.nick
        } ?: "<unknown>"
        return sourcePlayerNick
    }
}