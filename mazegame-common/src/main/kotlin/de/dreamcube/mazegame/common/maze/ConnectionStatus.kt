package de.dreamcube.mazegame.common.maze

/**
 * Enum class for the connection state.
 */
enum class ConnectionStatus() {
    NOT_CONNECTED, CONNECTED, LOGGED_IN, SPECTATING, PLAYING, DYING, DEAD;

    override fun toString(): String = when (this) {
        NOT_CONNECTED, DEAD -> "Disconnected"
        CONNECTED -> "Logging in..."
        LOGGED_IN -> "Logged in"
        SPECTATING -> "Spectating"
        PLAYING -> "Playing"
        DYING -> "Logging out..."
    }
}