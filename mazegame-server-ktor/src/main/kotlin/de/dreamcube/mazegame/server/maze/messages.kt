package de.dreamcube.mazegame.server.maze

import de.dreamcube.mazegame.common.maze.*

internal fun String.asMessage() = Message(this)

fun createEmptyLastMessage() = "".asMessage()

// Login stuff
fun createServerVersionMessage() = listOf("MSRV", PROTOCOL_VERSION.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createWelcomeMessage(id: Int) = listOf("WELC", id.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createMazeHeaderMessage(width: Int, height: Int) = listOf("MAZE", width.toString(), height.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createJoinMessage(player: Player) = listOf("JOIN", player.id.toString(), player.nick)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createLeaveMessage(id: Int) = listOf("LEAV", id.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

// Logout stuff
fun createQuitMessage() = "QUIT".asMessage()

// Info messages
fun createErrorInfoMessage(errorCode: InfoCode) = listOf("INFO", errorCode.code)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createServerInfoMessage(message: String) = listOf("INFO", InfoCode.SERVER_MESSAGE.code, message)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createClientInfoMessage(message: String, sourceId: Int) = listOf("INFO", InfoCode.CLIENT_MESSAGE.code, message, sourceId)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createClientWhisperInfoMessage(message: String, sourceId: Int) = listOf("INFO", InfoCode.CLIENT_WHISPER.code, message, sourceId)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()


// Player position and score messages
private fun createPlayerPositionChangeMessage(player: Player, reason: PlayerPositionChangeReason): String =
    listOf("PPOS", player.id.toString(), player.x.toString(), player.y.toString(), player.viewDirection.shortName, reason.shortName)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR)

fun createPlayerPositionAppearMessage(player: Player) = createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.APPEAR).asMessage()
fun createPlayerPositionVanishMessage(player: Player) = createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.VANISH).asMessage()
fun createPlayerPositionTurnMessage(player: Player) = createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TURN).asMessage()
fun createPlayerPositionStepMessage(player: Player) = createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.MOVE).asMessage()
fun createPlayerTeleportMessage(player: Player) = createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TELEPORT).asMessage()

fun createPlayerTeleportByTrapMessage(player: Player) =
    listOf(createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TELEPORT), TeleportType.TRAP.shortName)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createPlayerTeleportByCollisionMessage(player: Player, otherPlayerId: Int) =
    listOf(createPlayerPositionChangeMessage(player, PlayerPositionChangeReason.TELEPORT), TeleportType.COLLISION.shortName, otherPlayerId.toString())
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
fun createReadyMessage() = "RDY.".asMessage()