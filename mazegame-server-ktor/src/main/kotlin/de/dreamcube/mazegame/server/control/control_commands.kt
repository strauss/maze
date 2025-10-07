package de.dreamcube.mazegame.server.control

import de.dreamcube.mazegame.common.maze.BaitType


data class TeleportCommandDto(val id: Int, val x: Int, val y: Int)

data class PutBaitCommandDto(val baitType: BaitType, val x: Int, val y: Int, val visible: Boolean = true, val reappearOffset: Long = 0L)
