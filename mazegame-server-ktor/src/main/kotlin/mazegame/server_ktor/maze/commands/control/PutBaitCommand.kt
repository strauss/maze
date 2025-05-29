package mazegame.server_ktor.maze.commands.control

import kotlinx.coroutines.CompletableDeferred
import mazegame.server_ktor.maze.BaitType
import mazegame.server_ktor.maze.MazeServer
import mazegame.server_ktor.maze.Message
import mazegame.server_ktor.maze.commands.Command
import mazegame.server_ktor.maze.createServerInfoMessage

class PutBaitCommand(
    val mazeServer: MazeServer,
    val type: BaitType,
    val x: Int,
    val y: Int,
    val visible: Boolean = true,
    val reappearOffset: Long = 0L,
    val optionalMessage: String? = null
) :
    Command {
    val reply: CompletableDeferred<OccupationResult> = CompletableDeferred()

    override suspend fun execute() {
        try {
            val (result: OccupationResult, message: Message?) = mazeServer.putBait(type, x, y, visible, reappearOffset)
            if (result == OccupationResult.SUCCESS && message != null) {
                // When creating invisible baits, no message should be returned, despite the fact that it was successful
                if (optionalMessage == null) {
                    mazeServer.sendToAllPlayers(message)
                } else {
                    mazeServer.sendToAllPlayers(message.thereIsMore(), createServerInfoMessage(optionalMessage))
                }
            }
            reply.complete(result)
        } catch (e: Throwable) {
            reply.completeExceptionally(e)
            throw e
        }
    }
}