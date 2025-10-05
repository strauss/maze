package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.COMMAND_AND_MESSAGE_SEPARATOR
import de.dreamcube.mazegame.common.maze.Message

internal fun String.asMessage() = Message(this)

fun createEmptyLastMessage() = "".asMessage()

// Login stuff
fun createHelloMessage(nick: String) = listOf("HELO", nick).joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createRequestMazeMessage() = "MAZ?".asMessage()