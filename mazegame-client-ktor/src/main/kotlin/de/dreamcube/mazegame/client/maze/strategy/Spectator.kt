package de.dreamcube.mazegame.client.maze.strategy

@Bot("spectator", isSpectator = true, flavor = "Who will win? Nobody knows!")
class Spectator : Strategy() {

    /**
     * A spectator does ... nothing
     */
    override fun getNextMove(): Move = Move.DO_NOTHING

}