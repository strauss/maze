package de.dreamcube.mazegame.server.maze.game_events

import de.dreamcube.mazegame.server.maze.ServerBait
import de.dreamcube.mazegame.server.maze.ServerPlayer

interface GameEvent

data class BaitCollectedEvent(val bait: ServerBait, val player: ServerPlayer) : GameEvent

data class PlayerCollisionEvent(val causingPlayer: ServerPlayer, val otherPlayer: ServerPlayer, val x: Int, val y: Int) : GameEvent