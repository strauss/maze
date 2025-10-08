package de.dreamcube.mazegame.server.maze.game_events

import de.dreamcube.mazegame.common.maze.Player
import de.dreamcube.mazegame.server.maze.ServerBait

interface GameEvent

data class BaitCollectedEvent(val bait: ServerBait, val player: Player) : GameEvent

data class PlayerCollisionEvent(val causingPlayer: Player, val otherPlayer: Player, val x: Int, val y: Int) : GameEvent