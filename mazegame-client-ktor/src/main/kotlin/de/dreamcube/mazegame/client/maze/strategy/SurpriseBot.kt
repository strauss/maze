package de.dreamcube.mazegame.client.maze.strategy

/**
 * Interface for a special server-sided bot. Don't bother implementing it, it is not meant for you ... except you want
 * to implement your own "frenzy bot".
 */
interface SurpriseBot {
    /**
     * If true, this bot is in "surprise mode". If in this mode the server knows what to do :-)
     */
    fun surpriseModeActive(): Boolean
}