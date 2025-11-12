package de.dreamcube.mazegame.common.api

/**
 * Represents a JWT [token] and its [expires] time in milliseconds. It is used for bearer authentication when sending
 * server control commands.
 */
data class JwtToken(val token: String, val expires: Long)
