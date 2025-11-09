package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.MAX_FLAVOR_LENGTH
import de.dreamcube.mazegame.common.maze.MAX_NICK_LENGTH
import de.dreamcube.mazegame.common.maze.isNickValid
import de.dreamcube.mazegame.server.maze.ClientConnection
import de.dreamcube.mazegame.server.maze.MazeServer

class HelloCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {
    private val nick: String
    private val flavor: String?

    init {
        if (clientConnection.loggedIn()) {
            nick = ""
            flavor = null
            errorCode = InfoCode.ALREADY_LOGGED_IN
        } else if (commandWithParameters.size < 2) {
            nick = ""
            flavor = null
            errorCode = InfoCode.PARAMETER_COUNT_INCORRECT
        } else {
            nick = commandWithParameters[1]
            flavor =
                if (commandWithParameters.size > 2) commandWithParameters[2].trimToSize(MAX_FLAVOR_LENGTH) else null
        }
    }

    private fun String.trimToSize(maxLength: Int): String = if (this.length <= maxLength) this else
        "${this.take(maxLength - 3)}..."

    override suspend fun internalExecute() {
        // we can't check the nick in the constructor, so we do it here
        if (!isNickValid(nick) || nick.length > MAX_NICK_LENGTH) {
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
            mazeServer.registerClient(clientConnection, nick, flavor)
        }
    }
}