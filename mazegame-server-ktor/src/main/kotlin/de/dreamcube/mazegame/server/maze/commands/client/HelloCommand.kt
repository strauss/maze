package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer

class HelloCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {
    private val nick: String

    init {
        if (clientConnection.loggedIn()) {
            nick = ""
            errorCode = InfoCode.ALREADY_LOGGED_IN
        } else if (commandWithParameters.size != 2) {
            nick = ""
            errorCode = InfoCode.PARAMETER_COUNT_INCORRECT
        } else {
            nick = commandWithParameters[1]
        }
    }

    override suspend fun internalExecute() {
        // we can't check the nick in the constructor, so we do it here
        if (!isNickValid(nick)) {
            errorCode = InfoCode.WRONG_PARAMETER_VALUE
        } else if (mazeServer.containsNick(nick)) {
            errorCode =
                if (nick == mazeServer.serverConfiguration.serverBots.specialBots.trapeater ||
                    nick == mazeServer.serverConfiguration.serverBots.specialBots.frenzy
                )
                    InfoCode.WRONG_PARAMETER_VALUE
                else
                    InfoCode.DUPLICATE_NICK

        }
        if (okay) {
            mazeServer.registerClient(clientConnection, nick)
        }
    }

    private fun isNickValid(nick: String): Boolean = "[a-zA-Z][a-zA-Z0-9]*".toRegex().matches(nick)
}