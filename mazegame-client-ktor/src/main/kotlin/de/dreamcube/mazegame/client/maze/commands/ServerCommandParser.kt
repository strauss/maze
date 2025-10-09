package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.COMMAND_AND_MESSAGE_SEPARATOR
import de.dreamcube.mazegame.common.maze.CommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ServerCommandParser(parentScope: CoroutineScope, val mazeClient: MazeClient, val commandExecutor: CommandExecutor) :
    CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ServerCommandParser::class.java)

        private enum class InternalParserState {
            NO_MAZE, RECEIVING_MAZE, RECEIVED_MAZE
        }
    }

    private var internalState = InternalParserState.NO_MAZE
    private val inputChannel = Channel<String>(Channel.Factory.UNLIMITED)
    private var width = 0
    private var height = 0
    private val mazeLines: MutableList<String> = ArrayList()

    suspend fun receive(line: String) {
        inputChannel.send(line)
    }

    fun start() = launch {
        for (rawCommand in inputChannel) {
            val commandWithParameters: List<String> = rawCommand.split(COMMAND_AND_MESSAGE_SEPARATOR)
            if (commandWithParameters.isEmpty()) {
                LOGGER.error("Received empty command ... ignoring!")
                continue
            }
            val command: String = commandWithParameters[0]
            try {
                when (command) {
                    "PPOS" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(PlayerPosCommand(mazeClient, commandWithParameters))
                    }

                    "RDY." -> {
                        finalizeMazeCommand()
                    }

                    "BPOS" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(BaitPosCommand(mazeClient, commandWithParameters))
                    }

                    "PSCO" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(PlayerScoreCommand(mazeClient, commandWithParameters))
                    }

                    "INFO" -> {
                        finalizeMazeCommand()
                    }

                    "JOIN" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(JoinCommand(mazeClient, commandWithParameters))
                    }

                    "LEAV" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(LeaveCommand(mazeClient, commandWithParameters))
                    }

                    "MSRV" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(ProtocolVersionCommand(mazeClient, commandWithParameters))
                    }

                    "WELC" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(WelcomeCommand(mazeClient, commandWithParameters))
                    }

                    "MAZE" -> prepareMazeCommand(commandWithParameters)

                    "QUIT" -> {
                        finalizeMazeCommand()
                    }

                    else -> appendMazeCommand(commandWithParameters)
                }
            } catch (ex: Exception) {
                LOGGER.error("An error occurred while parsing the command '$rawCommand' ... ignoring!", ex)
            }
        }
    }

    private fun prepareMazeCommand(commandWithParameters: List<String>) {
        check(internalState == InternalParserState.NO_MAZE) { "Received MAZE command while in state $internalState." }
        check(commandWithParameters.size >= 3) { "Malformed MAZE command detected!" }
        width = commandWithParameters[1].toInt()
        height = commandWithParameters[2].toInt()
        internalState = InternalParserState.RECEIVING_MAZE
    }

    private fun appendMazeCommand(commandWithParameters: List<String>) {
        check(internalState == InternalParserState.RECEIVING_MAZE) { "Received maze line while in state $internalState." }
        check(commandWithParameters.size == 1) { "Received line was not a maze line." }
        mazeLines.add(commandWithParameters[0].trim())
    }

    private suspend fun finalizeMazeCommand() {
        if (internalState == InternalParserState.RECEIVING_MAZE) {
            val mazeCommand = MazeCommand(mazeClient, width, height, ArrayList<String>(mazeLines))
            mazeLines.clear()
            commandExecutor.addCommand(mazeCommand)
            internalState = InternalParserState.RECEIVED_MAZE
        }
    }
}
