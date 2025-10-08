package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.Player
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A collection of [Player]s.
 */
class PlayerCollection {
    /**
     * [Mutex] for thread- ... coroutine-safety
     */
    private val playerMutex = Mutex()

    /**
     * The internal structure holding all the [Player]s. The [MutableMap] is used for performance reasons.
     */
    private val playerIdToPlayerMap: MutableMap<Int, Player> = LinkedHashMap()

    /**
     * Adds a new [Player] to this [PlayerCollection]. It should only be called whenever a player appears.
     */
    internal suspend fun addPlayer(player: Player): Boolean {
        playerMutex.withLock {
            if (!playerIdToPlayerMap.containsKey(player.id)) {
                playerIdToPlayerMap[player.id] = player
                return true
            }
            return false
        }
    }

    /**
     * Removes the [Player] with the given [playerId]. It should only be called whenever a player vanishes.
     */
    internal suspend fun removePlayerById(playerId: Int): Boolean {
        playerMutex.withLock {
            return playerIdToPlayerMap.remove(playerId) != null
        }
    }

    /**
     * Retrieves a read-only [PlayerView] of the [Player] with the id [playerId]. If no such [Player] exists, null is returned. The view will directly
     * reflect all changes made to the player object and access will not be thread-safe.
     */
    internal fun getPlayerViewById(playerId: Int): PlayerView? {
        return playerIdToPlayerMap[playerId]?.let { PlayerView(it) }
    }

    /**
     * Retrieves a [PlayerSnapshot] of the current [Player] state.
     */
    internal suspend fun getPlayerSnapshot(playerId: Int): PlayerSnapshot? {
        playerMutex.withLock {
            return getPlayerViewById(playerId)?.let { PlayerSnapshot(it) }
        }
    }

    /**
     * Returns an immutable [List] of [PlayerSnapshot]s of all players that are currently playing. If the map changes or the player change, those
     * changes won't be reflected. Each [PlayerSnapshot] contains a read-only view of the actual [Player] object reflecting at least the player
     * changes.
     */
    internal suspend fun elements(): List<PlayerSnapshot> {
        playerMutex.withLock {
            return playerIdToPlayerMap.values.asSequence()
                .map { PlayerSnapshot(PlayerView(it)) }
                .toList()
        }
    }
}