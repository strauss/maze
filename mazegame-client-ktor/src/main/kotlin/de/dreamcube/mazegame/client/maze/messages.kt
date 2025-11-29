/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.COMMAND_AND_MESSAGE_SEPARATOR
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.Message

internal fun String.asMessage() = Message(this)

private val stepMessage = "STEP".asMessage()
private val turnLeftMessage = "TURN;l".asMessage()
private val turnRightMessage = "TURN;r".asMessage()

// Login stuff
fun createHelloMessage(nick: String, flavor: String?) = listOf("HELO", nick, flavor)
    .asSequence()
    .filter { it?.isNotBlank() ?: false }
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

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
    listOf("INFO", InfoCode.CLIENT_WHISPER.code, message, receiverPlayerId)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()
