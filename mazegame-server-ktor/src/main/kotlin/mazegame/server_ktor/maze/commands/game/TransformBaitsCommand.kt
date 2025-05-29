package mazegame.server_ktor.maze.commands.game

import kotlinx.coroutines.sync.withLock
import mazegame.server_ktor.maze.*
import mazegame.server_ktor.maze.commands.ServerSideCommand

class TransformBaitsCommand(mazeServer: MazeServer, val baitType: BaitType, val causingPlayerId: Int? = null) : ServerSideCommand(mazeServer) {
    override suspend fun execute() {
        if (baitType == BaitType.TRAP && !mazeServer.autoTrapeaterEnabled) {
            return
        }
        val messages: List<Message> = buildList {
            mazeServer.baitMutex.withLock {
                mazeServer.baitsById.values.forEach {
                    addAll(mazeServer.changeBait(it, baitType))
                }
            }
            if (isNotEmpty()) {
                val causingPlayerNick: String = mazeServer.getClientConnection(causingPlayerId)?.nick ?: "someone"
                when (baitType) {
                    BaitType.FOOD -> add(createServerInfoMessage("Oh no, $causingPlayerNick accidentally drank a cup of coffee from the office machine. All baits have been transformed to food.").thereIsMore())
                    BaitType.COFFEE -> add(createServerInfoMessage("Well ... $causingPlayerNick is so tired, they let all baits turn into the most delicious black coffee.").thereIsMore())
                    BaitType.GEM -> add(createServerInfoMessage("Yeah baby! It seems that $causingPlayerNick collected an enchanted golden apple. All baits have been transformed into gems.").thereIsMore())
                    BaitType.TRAP -> add(createServerInfoMessage("Oh no, $causingPlayerNick collected a blood diamond. All baits have turned into traps. But no worries, help has already arrived.").thereIsMore())
                }
                add(createEmptyLastMessage())
            }
        }
        if (messages.isNotEmpty()) {
            mazeServer.sendToAllPlayers(*messages.toTypedArray())
        }
        if (baitType == BaitType.TRAP) {
            mazeServer.autoTrapeaterHandler.spawn()
        } else {
            mazeServer.autoTrapeaterHandler.despawn()
        }
    }
}