package de.dreamcube.mazegame.common.maze

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

class CommandExecutor(private val parentScope: CoroutineScope) :
    CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CommandExecutor::class.java)
    }

    private val commandChannel = Channel<Command>(Channel.Factory.UNLIMITED)

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