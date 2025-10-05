package de.dreamcube.mazegame.server.maze.server_bots

/**
 * Allows for server side clients. Currently, only the legacy clients are possible, but this interface is "future-proof".
 */
interface ServerSideClient {

    /**
     * The id of the client to allow the server a correlation with player IDs
     */
    val clientId: Int

    /**
     * Allows the server to terminate the client from the server side.
     */
    fun terminate()

    /**
     * Allows the server to check if the bot has its "special mode" active. Currently, this can only be the frenzy mode of the frenzy bot. The special
     * mode allows the server to accelerate the bot if it is meant to "cheat".
     */
    val specialModeActive: Boolean

    /**
     * Indicates that the client could not connect.
     */
    val loginFailed: Boolean
}