package mazegame.server_ktor.maze

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mazegame.server_ktor.maze.commands.Command
import org.slf4j.LoggerFactory

class ServerCommandExecutor(private val parentScope: CoroutineScope) :
    CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ServerCommandExecutor::class.java)
    }

    private val commandChannel = Channel<Command>(Channel.UNLIMITED)

    /**
     * This mutex ensures that only one command is executed at a time.
     */
    private val commandMutex = Mutex()

    fun start() = launch {
        LOGGER.info("Command executor started!")
        for (command in commandChannel) {
            commandMutex.withLock {
                try {
                    command.execute()
                } catch (ex: Exception) {
                    LOGGER.error("Error while executing command: ", ex)
                }
            }
        }
        LOGGER.info("Command executor stopped!")
    }

    suspend fun addCommand(command: Command) = commandChannel.send(command)

}