package de.dreamcube.mazegame.client.maze

import de.dreamcube.mazegame.common.maze.Player
import de.dreamcube.mazegame.common.maze.PlayerPositionChangeReason
import de.dreamcube.mazegame.common.maze.ViewDirection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A collection of [Player]s.
 */
class PlayerCollection {

    companion object {
        private val MOVE_REASONS: Set<PlayerPositionChangeReason> = setOf(PlayerPositionChangeReason.MOVE, PlayerPositionChangeReason.TURN)
    }

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
     * Changes the [Player]'s position according to the given coordinates ([newX] and [newY]) and the [newViewDirection]. A [Pair] consisting of the
     * [PlayerSnapshot] before the change and the [PlayerSnapshot] after the change is returned. If the [Player] with the given [playerId] does not
     * exist, null is returned. If the reason is either a STEP or TURN, the [Player]'s move counter is increased for calculating the ms/move.
     */
    internal suspend fun changePlayerPosition(
        playerId: Int,
        newX: Int,
        newY: Int,
        newViewDirection: ViewDirection,
        reason: PlayerPositionChangeReason
    ): Pair<PlayerSnapshot, PlayerSnapshot>? {
        playerMutex.withLock {
            val player: Player = playerIdToPlayerMap[playerId] ?: return null
            val playerView = PlayerView(player)
            val oldSnapshot = PlayerSnapshot(playerView)
            player.x = newX
            player.y = newY
            player.viewDirection = newViewDirection
            if (reason in MOVE_REASONS) {
                player.incrementMoveCounter()
            }
            val newSnapshot = PlayerSnapshot(playerView)
            return Pair(oldSnapshot, newSnapshot)
        }
    }

    /**
     * Changes the [Player]'s score to the given [newScore]. Returns the old score. Returns null, if no player with [playerId] exists in the
     * collection.
     */
    internal suspend fun changePlayerScore(playerId: Int, newScore: Int): Int? {
        playerMutex.withLock {
            val player: Player = playerIdToPlayerMap[playerId] ?: return null
            val oldScore = player.score
            player.score = newScore
            return oldScore
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