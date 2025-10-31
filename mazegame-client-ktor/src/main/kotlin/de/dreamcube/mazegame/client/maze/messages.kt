package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.COMMAND_AND_MESSAGE_SEPARATOR
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.Message

internal fun String.asMessage() = Message(this)

private val stepMessage = "STEP".asMessage()
private val turnLeftMessage = "TURN;l".asMessage()
private val turnRightMessage = "TURN;r".asMessage()

fun createEmptyLastMessage() = "".asMessage()

// Login stuff
fun createHelloMessage(nick: String) = listOf("HELO", nick).joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createRequestMazeMessage() = "MAZ?".asMessage()

// Logout stuff
fun createByeMessage() = "BYE!".asMessage()

// Movement
fun createStepMessage() = stepMessage
fun createTurnLeftMessage() = turnLeftMessage
fun createTurnRightMessage() = turnRightMessage

// Chat
fun createChatMessage(message: String) = listOf("INFO", InfoCode.CLIENT_MESSAGE.code, message)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createWhisperMessage(message: String, receiverPlayerId: Int) =
    listOf("INFO", InfoCode.CLIENT_WHISPER, message, receiverPlayerId)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()
