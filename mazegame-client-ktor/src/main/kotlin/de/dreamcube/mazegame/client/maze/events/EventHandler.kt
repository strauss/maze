package de.dreamcube.mazegame.client.maze.events

import de.dreamcube.mazegame.client.maze.Bait
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.common.maze.InfoCode
import de.dreamcube.mazegame.common.maze.TeleportType
import java.util.*

/**
 * Central class for handling all maze game client related events.
 */
class EventHandler {

    // All the lists separated by listener types
    private val baitEventListeners: MutableList<BaitEventListener> = LinkedList()
    private val playerConnectionListeners: MutableList<PlayerConnectionListener> = LinkedList()
    private val playerMovementListeners: MutableList<PlayerMovementListener> = LinkedList()
    private val scoreChangeListeners: MutableList<ScoreChangeListener> = LinkedList()
    private val chatInfoListeners: MutableList<ChatInfoListener> = LinkedList()
    private val errorInfoListeners: MutableList<ErrorInfoListener> = LinkedList()

    /**
     * Adds the given [EventListener] to all matching lists according to the implemented interfaces. This operation costs O(1).
     */
    fun addEventListener(listener: EventListener) {
        // Feel the power of smart casts :-)
        if (listener is BaitEventListener) {
            baitEventListeners.add(listener)
        }
        if (listener is PlayerConnectionListener) {
            playerConnectionListeners.add(listener)
        }
        if (listener is PlayerMovementListener) {
            playerMovementListeners.add(listener)
        }
        if (listener is ScoreChangeListener) {
            scoreChangeListeners.add(listener)
        }
        if (listener is ChatInfoListener) {
            chatInfoListeners.add(listener)
        }
        if (listener is ErrorInfoListener) {
            errorInfoListeners.add(listener)
        }
    }

    /**
     * Removes the given [EventListener] from all matching lists according to the implemented interfaces. This operation costs O(n).
     */
    fun removeEventListener(listener: EventListener) {
        // Feel the power of smart casts :-)
        if (listener is BaitEventListener) {
            baitEventListeners.remove(listener)
        }
        if (listener is PlayerConnectionListener) {
            playerConnectionListeners.remove(listener)
        }
        if (listener is PlayerMovementListener) {
            playerMovementListeners.remove(listener)
        }
        if (listener is ScoreChangeListener) {
            scoreChangeListeners.remove(listener)
        }
        if (listener is ChatInfoListener) {
            chatInfoListeners.remove(listener)
        }
        if (listener is ErrorInfoListener) {
            errorInfoListeners.remove(listener)
        }
    }

    // All the events
    fun fireBaitAppeared(bait: Bait) {
        for (listener in baitEventListeners) {
            listener.onBaitAppeared(bait)
        }
    }

    fun fireBaitVanished(bait: Bait) {
        for (listener in baitEventListeners) {
            listener.onBaitVanished(bait)
        }
    }

    fun firePlayerLogin(playerId: Int, nick: String) {
        for (listener in playerConnectionListeners) {
            listener.onPlayerLogin(playerId, nick)
        }
    }

    fun firePlayerLogout(playerId: Int) {
        for (listener in playerConnectionListeners) {
            listener.onPlayerLogout(playerId)
        }
    }

    fun firePlayerAppear(player: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerAppear(player)
        }
    }

    fun firePlayerVanish(player: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerVanish(player)
        }
    }

    fun firePlayerStep(oldPosition: PlayerSnapshot, newPosition: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerStep(oldPosition, newPosition)
        }
    }

    fun firePlayerTurn(oldPosition: PlayerSnapshot, newPosition: PlayerSnapshot) {
        for (listener in playerMovementListeners) {
            listener.onPlayerTurn(oldPosition, newPosition)
        }
    }

    fun firePlayerTeleport(oldPosition: PlayerSnapshot, newPosition: PlayerSnapshot, teleportType: TeleportType?, otherPlayerId: Int?) {
        for (listener in playerMovementListeners) {
            listener.onPlayerTeleport(oldPosition, newPosition, teleportType, otherPlayerId)
        }
    }

    fun fireScoreChange(playerId: Int, oldScore: Int, newScore: Int) {
        for (listener in scoreChangeListeners) {
            listener.onScoreChange(playerId, oldScore, newScore)
        }
    }

    fun fireClientInfo(message: String) {
        for (listener in chatInfoListeners) {
            listener.onClientInfo(message)
        }
    }

    fun fireServerInfo(message: String) {
        for (listener in chatInfoListeners) {
            listener.onServerInfo(message)
        }
    }

    fun firePlayerChat(playerId: Int, playerNick: String, message: String, whisper: Boolean) {
        for (listener in chatInfoListeners) {
            listener.onPlayerChat(playerId, playerNick, message, whisper)
        }
    }

    fun fireServerError(infoCode: InfoCode) {
        for (listener in errorInfoListeners) {
            listener.onServerError(infoCode)
        }
    }
}