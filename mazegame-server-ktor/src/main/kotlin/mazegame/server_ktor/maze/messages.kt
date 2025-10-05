package mazegame.server_ktor.maze

import de.dreamcube.mazegame.common.maze.Message
import mazegame.server_ktor.maze.commands.client.ChatCommand

internal fun String.asMessage() = Message(this)

fun createEmptyLastMessage() = "".asMessage()

// Login stuff
fun createServerVersionMessage() = listOf("MSRV", PROTOCOL_VERSION.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createWelcomeMessage(id: Int) = listOf("WELC", id.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createMazeHeaderMessage(width: Int, height: Int) = listOf("MAZE", width.toString(), height.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createJoinMessage(player: ServerPlayer) = listOf("JOIN", player.id.toString(), player.nick)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createLeaveMessage(id: Int) = listOf("LEAV", id.toString())
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

// Logout stuff
fun createQuitMessage() = "QUIT".asMessage()

// Info messages
fun createInfoMessage(errorCode: ErrorCode) = listOf("INFO", errorCode.code)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createServerInfoMessage(message: String) = listOf("INFO", ChatCommand.SERVER_MESSAGE_CODE, message)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createClientInfoMessage(message: String, sourceId: Int) = listOf("INFO", ChatCommand.CLIENT_MESSAGE_CODE, message, sourceId)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createClientWhisperInfoMessage(message: String, sourceId: Int) = listOf("INFO", ChatCommand.CLIENT_WHISPER_CODE, message, sourceId)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()


// Player position and score messages
private fun createPlayerPositionChangeMessage(player: ServerPlayer, reason: PlayerPositionChange): String =
    listOf("PPOS", player.id.toString(), player.x.toString(), player.y.toString(), player.viewDirection.shortName, reason.shortName)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR)

fun createPlayerPositionAppearMessage(player: ServerPlayer) = createPlayerPositionChangeMessage(player, PlayerPositionChange.APPEAR).asMessage()
fun createPlayerPositionVanishMessage(player: ServerPlayer) = createPlayerPositionChangeMessage(player, PlayerPositionChange.VANISH).asMessage()
fun createPlayerPositionTurnMessage(player: ServerPlayer) = createPlayerPositionChangeMessage(player, PlayerPositionChange.TURN).asMessage()
fun createPlayerPositionStepMessage(player: ServerPlayer) = createPlayerPositionChangeMessage(player, PlayerPositionChange.MOVE).asMessage()
fun createPlayerTeleportMessage(player: ServerPlayer) = createPlayerPositionChangeMessage(player, PlayerPositionChange.TELEPORT).asMessage()

fun createPlayerTeleportByTrapMessage(player: ServerPlayer) =
    listOf(createPlayerPositionChangeMessage(player, PlayerPositionChange.TELEPORT), TeleportType.TRAP.shortName)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createPlayerTeleportByCollisionMessage(player: ServerPlayer, otherPlayerId: Int) =
    listOf(createPlayerPositionChangeMessage(player, PlayerPositionChange.TELEPORT), TeleportType.COLLISION.shortName, otherPlayerId.toString())
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createPlayerScoreChangedMessage(player: ServerPlayer) = listOf("PSCO", player.id.toString(), player.score)
    .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()


// Bait message
private fun createBaitPositionChangeMessage(bait: ServerBait, reason: BaitPositionChange) =
    listOf("BPOS", bait.x.toString(), bait.y.toString(), bait.type.baitName, reason.shortName)
        .joinToString(COMMAND_AND_MESSAGE_SEPARATOR).asMessage()

fun createBaitGeneratedMessage(bait: ServerBait) = createBaitPositionChangeMessage(bait, BaitPositionChange.GENERATED)
fun createBaitCollectedMessage(bait: ServerBait) = createBaitPositionChangeMessage(bait, BaitPositionChange.COLLECTED)


// Ready message
fun createReadyMessage() = "RDY.".asMessage()