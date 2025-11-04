package de.dreamcube.mazegame.client.maze.events

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.common.maze.ConnectionStatus
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.PlayerPosition
import de.dreamcube.mazegame.common.maze.TeleportType

sealed interface EventListener

/**
 * Dummy-Interface for allowing the abstract superclass [de.dreamcube.mazegame.client.maze.strategy.Strategy] to have no specific [EventListener]
 * implemented. Does not contain any functions.
 */
interface NoEventListener : EventListener

/**
 * This listener interface handles maze related events.
 */
interface MazeEventListener : EventListener {
    /**
     * After the server sent the whole maze data, this method is called and should be used to create the desired internal maze structure for the
     * strategy.
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
     * This function is called, when a player was teleported.
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
 * This listener interface handles errors sent by the server.
 */
interface ErrorInfoListener : EventListener {
    /**
     * This function is called, when the server sends an error-related [infoCode].
     */
    fun onServerError(infoCode: InfoCode)
}

/**
 * This listener interface is intended for the UI for directly reacting to connection status changes.
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