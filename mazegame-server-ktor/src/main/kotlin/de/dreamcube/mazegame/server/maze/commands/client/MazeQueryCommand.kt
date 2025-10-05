package de.dreamcube.mazegame.server.maze.commands.client

import de.dreamcube.mazegame.server.maze.*

class MazeQueryCommand(clientConnection: ClientConnection, mazeServer: MazeServer, commandWithParameters: List<String>) :
    ClientCommand(mazeServer, clientConnection) {

    init {
        if (commandWithParameters.size != 1) {
            errorCode = ErrorCode.WRONG_PARAMETER_VALUE
        }
        checkLoggedIn()
    }

    override suspend fun internalExecute() {
        mazeServer.sendMapData(clientConnection)
        mazeServer.sendAllPlayerInfos(clientConnection)
        mazeServer.sendAllBaitInfos(clientConnection)
        if (clientConnection.startAsSpectator) {
            startSpectating()
        } else {
            startPlaying()
        }
    }

    private suspend fun startSpectating() {
        clientConnection.spectate()
        sendWelcomeMessages()
        val players = mazeServer.activePlayers.get()
        if (players <= 0) {
            clientConnection.sendMessage(createServerInfoMessage("There is nobody to spectate!"))
        } else if (players == 1) {
            clientConnection.sendMessage(createServerInfoMessage("There is one payer playing."))
        } else {
            clientConnection.sendMessage(createServerInfoMessage("There are $players players playing."))
        }
    }

    private suspend fun startPlaying() {
        clientConnection.play()
        clientConnection.ready()
        sendWelcomeMessages()
        val otherPlayers = mazeServer.activePlayers.get() - 1
        if (otherPlayers <= 0) {
            clientConnection.sendMessage(createServerInfoMessage("You are alone ... for now!").thereIsMore())
        } else if (otherPlayers == 1) {
            clientConnection.sendMessage(createServerInfoMessage("One other player is already having fun.").thereIsMore())
        } else {
            clientConnection.sendMessage(createServerInfoMessage("$otherPlayers players are already having fun.").thereIsMore())
        }
        clientConnection.sendMessage(ClientChatControl.FIRST_CHAT_HINT)
    }

    private suspend fun sendWelcomeMessages() {
        clientConnection.sendMessage(createServerInfoMessage("Welcome to this amazing maze!").thereIsMore())
        clientConnection.sendMessage(createServerInfoMessage("The dimensions are ${mazeServer.maze.width} x ${mazeServer.maze.height}.").thereIsMore())
        clientConnection.sendMessage(createServerInfoMessage("The number of walkable fields is ${mazeServer.positionProvider.walkablePositionsSize}.").thereIsMore())
    }
}