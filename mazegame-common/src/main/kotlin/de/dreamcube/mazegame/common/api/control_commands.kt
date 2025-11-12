package de.dreamcube.mazegame.common.api

import de.dreamcube.mazegame.common.maze.BaitType

/**
 * The server control command for teleportation.
 */
data class TeleportCommandDto(val id: Int, val x: Int, val y: Int)

/**
 * The server control command for putting a bait.
 */
data class PutBaitCommandDto(
    val baitType: BaitType,
    val x: Int,
    val y: Int,
    val visible: Boolean = true,
    val reappearOffset: Long = 0L
)
