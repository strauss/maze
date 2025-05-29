package mazegame.server_ktor.maze.game_events

import mazegame.server_ktor.maze.ServerBait
import mazegame.server_ktor.maze.ServerPlayer

interface GameEvent

data class BaitCollectedEvent(val bait: ServerBait, val player: ServerPlayer) : GameEvent

data class PlayerCollisionEvent(val causingPlayer: ServerPlayer, val otherPlayer: ServerPlayer, val x: Int, val y: Int) : GameEvent