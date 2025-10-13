package de.dreamcube.mazegame.client.maze.strategy

/**
 * Interface for a special server-sided bot.
 */
interface SurpriseBot {
    /**
     * If true, this Bot is in "surprise mode". If in this mode the server knows what to do :-)
     */
    fun surpriseModeActive(): Boolean
}