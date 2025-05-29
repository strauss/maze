package mazegame.server_ktor.maze.commands.game

import mazegame.server_ktor.maze.MazeServer
import mazegame.server_ktor.maze.Message
import mazegame.server_ktor.maze.commands.ServerSideCommand
import mazegame.server_ktor.maze.createEmptyLastMessage
import mazegame.server_ktor.maze.createServerInfoMessage

class BaitRushCommand(mazeServer: MazeServer, val causingPlayerId: Int? = null) : ServerSideCommand(mazeServer) {
    override suspend fun execute() {
        val baseBaitCount: Int = mazeServer.baseBaitCount
        val desiredBaitCount: Int = mazeServer.desiredBaitCount.get()
        if (baseBaitCount == desiredBaitCount) {
            mazeServer.desiredBaitCount.set(baseBaitCount * 2)
            val messages: List<Message> = buildList {
                addAll(mazeServer.replaceBaits())
                val causingPlayerNick: String = mazeServer.getClientConnection(causingPlayerId)?.nick ?: "someone"
                add(createServerInfoMessage("Nice one! It seems $causingPlayerNick stepped on an invisible pressure plate and caused more baits to spawn ... at least temporarily."))
                add(createEmptyLastMessage())
            }
            mazeServer.desiredBaitCount.set(baseBaitCount)
            mazeServer.sendToAllPlayers(*messages.toTypedArray())
        }
    }
}