package de.dreamcube.mazegame.client.maze.commands

import de.dreamcube.mazegame.client.maze.MazeClient
import de.dreamcube.mazegame.common.maze.COMMAND_AND_MESSAGE_SEPARATOR
import de.dreamcube.mazegame.common.maze.CommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * This class is responsible for parsing server commands.
 */
class ServerCommandParser(
    parentScope: CoroutineScope,
    val mazeClient: MazeClient,
    val commandExecutor: CommandExecutor
) :
    CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ServerCommandParser::class.java)

        private enum class InternalParserState {
            NO_MAZE, RECEIVING_MAZE, RECEIVED_MAZE
        }
    }

    /**
     * The internal state of the parser with respect to the receiving of maze data.
     */
    private var internalState = InternalParserState.NO_MAZE
    private val inputChannel = Channel<String>(Channel.Factory.UNLIMITED)

    /**
     * The width of the received maze.
     */
    private var width = 0

    /**
     * The height of the received maze.
     */
    private var height = 0

    /**
     * The list containing the lines of the received maze. They are collected here, while they are arriving.
     */
    private val mazeLines: MutableList<String> = ArrayList()

    suspend fun receive(line: String) {
        inputChannel.send(line)
    }

    /**
     * Contains the main loop of the command receiver in a coroutine. Also handles the receiving of the maze data
     * properly.
     *
     * Here the first parameter of each command is evaluated and the corresponding [ClientSideCommand] is generated.
     * If we are in [internalState] [InternalParserState.RECEIVING_MAZE], any non-matching command is considered a maze
     * line. If a matching command is received, the [internalState] switches to [InternalParserState.RECEIVED_MAZE] and
     * the [MazeCommand] is finalized.
     */
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
                        commandExecutor.addCommand(ReadyCommand(mazeClient))
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
                        commandExecutor.addCommand(InfoCommand(mazeClient, commandWithParameters))
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

                    "TERM" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(TermCommand(mazeClient))
                    }

                    "QUIT" -> {
                        finalizeMazeCommand()
                        commandExecutor.addCommand(QuitCommand(mazeClient))
                    }

                    else -> appendMazeCommand(commandWithParameters)
                }
            } catch (ex: Exception) {
                LOGGER.error("An error occurred while parsing the command '$rawCommand' ... ignoring!", ex)
            }
        }
    }

    /**
     * Switches from [internalState] [InternalParserState.NO_MAZE] to [InternalParserState.RECEIVING_MAZE]. The command
     * string already contains the dimensions of the maze, which are extracted here.
     */
    private fun prepareMazeCommand(commandWithParameters: List<String>) {
        check(internalState == InternalParserState.NO_MAZE) { "Received MAZE command while in state $internalState." }
        check(commandWithParameters.size >= 3) { "Malformed MAZE command detected!" }
        width = commandWithParameters[1].toInt()
        // TODO: we could use the height to improve the parsing process ... the goal should be avoiding the finalizeMazeCommand function
        height = commandWithParameters[2].toInt()
        internalState = InternalParserState.RECEIVING_MAZE
    }

    /**
     * Receives a line of maze map data.
     */
    private fun appendMazeCommand(commandWithParameters: List<String>) {
        check(internalState == InternalParserState.RECEIVING_MAZE) { "Received maze line while in state $internalState." }
        check(commandWithParameters.size == 1) { "Received line was not a maze line." }
        mazeLines.add(commandWithParameters[0].trim())
    }

    /**
     * Checks if we are still receiving maze data. Creates the complete [MazeCommand] object and transfers it to the
     * [commandExecutor].
     */
    private suspend fun finalizeMazeCommand() {
        if (internalState == InternalParserState.RECEIVING_MAZE) {
            val mazeCommand = MazeCommand(mazeClient, width, height, ArrayList<String>(mazeLines))
            mazeLines.clear()
            commandExecutor.addCommand(mazeCommand)
            internalState = InternalParserState.RECEIVED_MAZE
        }
    }
}
