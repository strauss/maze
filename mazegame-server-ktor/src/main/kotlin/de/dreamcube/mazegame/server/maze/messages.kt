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

package de.dreamcube.mazegame.server.maze

import de.dreamcube.mazegame.common.api.GameSpeed
import de.dreamcube.mazegame.common.maze.*

private val EMPTY_LAST_MESSAGE = "".asMessage()

private val RDY_MESSAGE = "RDY.".asMessage()

internal fun String.asMessage() = Message(this)

fun createEmptyLastMessage() = EMPTY_LAST_MESSAGE

// Login stuff
fun createServerVersionMessage() = listOf("MSRV", PROTOCOL_VERSION.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createWelcomeMessage(id: Int) = listOf("WELC", id.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createMazeHeaderMessage(width: Int, height: Int) = listOf("MAZE", width.toString(), height.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createJoinMessage(player: Player) = listOf("JOIN", player.id.toString(), player.nick, player.flavor)
    .asSequence()
    .filter { it?.isNotBlank() ?: false }
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createLeaveMessage(id: Int) = listOf("LEAV", id.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

// Logout stuff
/**
 * Logout confirmation.
 */
fun createQuitMessage() = "QUIT".asMessage()

/**
 * Server-side termination of the connection.
 */
fun createTermMessage() = "TERM".asMessage()

// Info messages
fun createErrorInfoMessage(errorCode: InfoCode) = listOf("INFO", errorCode.code)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createServerInfoMessage(message: String) = listOf("INFO", InfoCode.SERVER_MESSAGE.code, message)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createClientInfoMessage(message: String, sourceId: Int) =
    listOf("INFO", InfoCode.CLIENT_MESSAGE.code, message, sourceId)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createClientWhisperInfoMessage(message: String, sourceId: Int) =
    listOf("INFO", InfoCode.CLIENT_WHISPER.code, message, sourceId)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createSpeedChangeInfoMessage(newSpeed: GameSpeed) = listOf("INFO", InfoCode.SPEED_CHANGE.code, newSpeed.delay)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()


// Player position and score messages
private fun createPlayerPositionChangeMessage(player: Player, reason: PlayerPositionChangeReason): String =
    listOf(
        "PPOS",
        player.id.toString(),
        player.x.toString(),
        player.y.toString(),
        player.viewDirection.shortName,
        reason.shortName
    )
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR)

fun createPlayerPositionAppearMessage(player: Player) =
    createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.APPEAR).asMessage()

fun createPlayerPositionVanishMessage(player: Player) =
    createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.VANISH).asMessage()

fun createPlayerPositionTurnMessage(player: Player) =
    createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TURN).asMessage()

fun createPlayerPositionStepMessage(player: Player) =
    createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.MOVE).asMessage()

fun createPlayerTeleportMessage(player: Player) =
    createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TELEPORT).asMessage()

fun createPlayerTeleportByTrapMessage(player: Player) =
    listOf(createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TELEPORT), TeleportType.TRAP.shortName)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createPlayerTeleportByCollisionMessage(player: Player, causingPlayerId: Int) =
    listOf(
        createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TELEPORT),
        TeleportType.COLLISION.shortName,
        causingPlayerId.toString()
    )
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createPlayerScoreChangedMessage(player: Player) = listOf("PSCO", player.id.toString(), player.score)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()


// Bait message
private fun createBaitPositionChangeMessage(bait: ServerBait, reason: BaitPositionChange) =
    listOf("BPOS", bait.x.toString(), bait.y.toString(), bait.type.baitName, reason.shortName)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createBaitGeneratedMessage(bait: ServerBait) = createBaitPositionChangeMessage(bait, BaitPositionChange.GENERATED)
fun createBaitCollectedMessage(bait: ServerBait) = createBaitPositionChangeMessage(bait, BaitPositionChange.COLLECTED)

// Ready message
fun createReadyMessage() = RDY_MESSAGE