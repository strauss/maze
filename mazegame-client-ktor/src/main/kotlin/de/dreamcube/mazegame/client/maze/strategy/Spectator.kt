package de.dreamcube.mazegame.client.maze.strategy

@Bot("spectator", isSpectator = true)
class Spectator : Strategy() {

    /**
     * A spectator does ... nothing
     */
    override fun getNextMove(): Move = Move.DO_NOTHING

}