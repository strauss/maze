package de.dreamcube.mazegame.client

import de.dreamcube.mazegame.client.maze.events.ChatInfoListener
import de.dreamcube.mazegame.client.maze.events.ErrorInfoListener
import de.dreamcube.mazegame.common.maze.InfoCode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HeadlessChatDisplay : ChatInfoListener {
    private val LOGGER: Logger = LoggerFactory.getLogger(HeadlessChatDisplay::class.java)

    override fun onClientInfo(message: String) {
        LOGGER.info("Client: $message")
    }

    override fun onServerInfo(message: String) {
        LOGGER.info("Server: $message")
    }

    override fun onPlayerChat(playerId: Int, playerNick: String, message: String, whisper: Boolean) {
        val appendix: String = if (whisper) "(whisper)" else ""
        LOGGER.info("$playerNick$appendix: $message")
    }
}

object HeadlessErrorDisplay : ErrorInfoListener {
    private val LOGGER: Logger = LoggerFactory.getLogger(HeadlessErrorDisplay::class.java)
    override fun onServerError(infoCode: InfoCode) {
        val infoNumber = infoCode.code
        when (infoCode) {
            InfoCode.WRONG_PARAMETER_VALUE -> {
                LOGGER.warn("$infoNumber: Wrong parameter value")
            }

            InfoCode.TOO_MANY_CLIENTS -> {
                LOGGER.warn("$infoNumber: Server full (too many clients)")
            }

            InfoCode.DUPLICATE_NICK -> {
                LOGGER.warn("$infoNumber: Duplicate/Invalid nick")
            }

            InfoCode.WALL_CRASH -> {
                LOGGER.warn("$infoNumber: Tried to step into a wall")
            }

            InfoCode.ACTION_WITHOUT_READY -> {
                LOGGER.warn("$infoNumber: Did not wait for RDY.")
            }

            InfoCode.ALREADY_LOGGED_IN -> {
                LOGGER.warn("$infoNumber: Already logged in")
            }

            InfoCode.COMMAND_BEFORE_LOGIN -> {
                LOGGER.warn("$infoNumber: Not logged in")
            }

            InfoCode.LOGIN_TIMEOUT -> {
                LOGGER.error("$infoNumber: Login timed out")
            }

            InfoCode.UNKNOWN_COMMAND -> {
                LOGGER.error("$infoNumber: The server did not understand our last command")
            }

            InfoCode.PARAMETER_COUNT_INCORRECT -> {
                LOGGER.error("$infoNumber: Incorrect number of parameters")
            }

            InfoCode.COMPLETELY_UNKNOWN -> {
                LOGGER.error("$infoNumber: We do not know this error code")
            }

            else -> {
                LOGGER.error("${infoCode.name} should not be manifested as server error event.")
            }
        }
    }
}