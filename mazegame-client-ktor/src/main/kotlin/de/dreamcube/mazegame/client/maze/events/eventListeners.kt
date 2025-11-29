/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dreamcube.mazegame.client.maze.events

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType

/**
 * Super-interface for all client event listeners.
 */
sealed interface EventListener

/**
 * Dummy-Interface for allowing the abstract superclass [de.dreamcube.mazegame.client.maze.strategy.Strategy] to have no
 * specific [EventListener] implemented. Does not contain any functions.
 */
interface NoEventListener : EventListener

/**
 * This listener interface handles maze related events. If your strategy requires the maze's map, its class should
 * implement this interface.
 */
interface MazeEventListener : EventListener {
    /**
     * After the server sent the whole maze data, this method is called and should be used to create the desired
     * internal maze structure for the strategy. This function the perfect place for initializing strategy-related
     * data that requires the maze to be present.
     */
    fun onMazeReceived(width: Int, height: Int, mazeLines: List<String>)
}

/**
 * This listener interface handles bait related events.
 */
interface BaitEventListener : EventListener {
    /**
     * This function is called, whenever the server reports that a bait appeared.
     */
    fun onBaitAppeared(bait: Bait)

    /**
     * This function is called, whenever the serverreports that a bait has vanished.
     */
    fun onBaitVanished(bait: Bait)
}

/**
 * This listener interface handles the login status of the other players.
 */
interface PlayerConnectionListener : EventListener {
    /**
     * This function is called when the server reports, that a new player joined the game.
     */
    fun onPlayerLogin(playerSnapshot: PlayerSnapshot) {
        // does nothing by default
    }

    /**
     * This function is called when the server reports, that the "own" player joined the game. This function is called in addition to [onPlayerLogin].
     * It is useful for some strategies to obtain basic information about the player object of the own player.
     */
    fun onOwnPlayerLogin(playerSnapshot: PlayerSnapshot) {
        // does nothing by default
    }

    /**
     * This function is called, when the server reports, that a player left the game.
     */
    fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        // does nothing by default
    }
}

/**
 * This listener interface handles the position changes of all players (including the own player).
 */
interface PlayerMovementListener : EventListener {
    /**
     * This function is called, when a player position is communicated for the first time. It happens shortly after joining.
     */
    fun onPlayerAppear(playerSnapshot: PlayerSnapshot) {
        // does nothing by default
    }

    /**
     * This function is called, when a player is about to leave the game. It happens right before leaving.
     */
    fun onPlayerVanish(playerSnapshot: PlayerSnapshot) {
        // does nothing by default
    }

    /**
     * This function is called, when a player successfully performed a step move.
     */
    fun onPlayerStep(oldPosition: PlayerPosition, newPlayerSnapshot: PlayerSnapshot) {
        // does nothing by default
    }

    /**
     * This function is called, when a player successfully performed a turn move.
     */
    fun onPlayerTurn(oldPosition: PlayerPosition, newPlayerSnapshot: PlayerSnapshot) {
        // does nothing by default
    }

    /**
     * This function is called, when a player was teleported. The optional [teleportType] indicates, whether the
     * teleport was caused by a [TeleportType.TRAP] or a [TeleportType.COLLISION]. In the latter case, the
     * [causingPlayerId] contains the id of the responsible player (the one who crashed into the other one). The server
     * can always tell who is to blame :-)
     */
    fun onPlayerTeleport(
        oldPosition: PlayerPosition,
        newPlayerSnapshot: PlayerSnapshot,
        teleportType: TeleportType?,
        causingPlayerId: Int?
    ) {
        // does nothing by default
    }
}

/**
 * This listener interface handles score changes.
 */
interface ScoreChangeListener : EventListener {
    /**
     * This function is called, when a player's score changed. The new score is contained in the [newPlayerSnapshot].
     */
    fun onScoreChange(oldScore: Int, newPlayerSnapshot: PlayerSnapshot)
}

/**
 * This listener interface handles (chat) messages that are intended for being displayed in the console or on the UI.
 */
interface ChatInfoListener : EventListener {
    /**
     * This function is called, when the client itself wants to display something.
     */
    fun onClientInfo(message: String)

    /**
     * This function is called, when the server wants to display something.
     */
    fun onServerInfo(message: String)

    /**
     * This function is called, when another player wants to chat. If it is a [whisper]ed, message, the flag tells so.
     */
    fun onPlayerChat(playerId: Int, playerNick: String, message: String, whisper: Boolean)
}

/**
 * This listener interface handles errors sent by the server. The [InfoCode] for further information.
 */
interface ErrorInfoListener : EventListener {
    /**
     * This function is called, when the server sends an error-related [infoCode].
     */
    fun onServerError(infoCode: InfoCode)
}

/**
 * This listener interface is intended for the UI for directly reacting to connection status changes.
 *
 * In order to use it properly, you need to know what happens in each status and what is "available" in each status.
 *
 * - [ConnectionStatus.NOT_CONNECTED]: When the client is created it starts in this status. Here, the strategy object is
 * not available yet. The strategy object is created before a connection attempt is made.
 * - [ConnectionStatus.CONNECTED]: The connection has been established on the network level. The client expects the
 * first command from the server, namely the "MSRV" command. This command initializes the "handshake" process. After
 * receiving the "MSRV" command, the client responds with the "HELO" command. The server then either responds with an
 * error code or with a "WELC" command. The latter contains the client ID.
 * - [ConnectionStatus.LOGGED_IN]: In this status, the client ID is available. The next step for the client is
 * requesting the maze with the "MAZ?" command. After receiving the maze data, the [MazeEventListener.onMazeReceived]
 * event is fired.
 * - [ConnectionStatus.SPECTATING]: In this status, the maze data is available and the server starts sending game
 * updates to the client, including the login events for all players, including the own player's. If the client is
 * logged in as spectator (determined by a special nickname), the server won't send a "RDY." command.
 * - [ConnectionStatus.PLAYING]: This status is reached, when the server sends the first "RDY." command. Here, the
 * client is able to send movement related commands to the server.
 * - [ConnectionStatus.DYING]: This is literally a short-lived status. It indicates that the client is logging out of
 * the game (or the server terminated the connection).
 * - [ConnectionStatus.DEAD]: In this state the client has been disconnected and the game is over. In order to play
 * again, a new client has to be created.
 */
interface ClientConnectionStatusListener : EventListener {
    /**
     * This function is called, just after the [oldStatus] is changed to the [newStatus].
     */
    fun onConnectionStatusChange(oldStatus: ConnectionStatus, newStatus: ConnectionStatus)
}

/**
 * This listener interface is used for speed changes.
 */
interface SpeedChangedListener : EventListener {
    /**
     * This function is called, whenever the game speed changes.
     */
    fun onSpeedChanged(oldSpeed: Int, newSpeed: Int)
}