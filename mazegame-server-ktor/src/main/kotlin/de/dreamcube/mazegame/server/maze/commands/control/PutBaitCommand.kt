package de.dreamcube.mazegame.server.maze.commands.control

import de.dreamcube.mazegame.common.maze.Command
import de.dreamcube.mazegame.common.maze.Message
import de.dreamcube.mazegame.server.maze.BaitType
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.createServerInfoMessage
import kotlinx.coroutines.CompletableDeferred

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