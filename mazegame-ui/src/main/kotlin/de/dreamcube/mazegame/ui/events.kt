package de.dreamcube.mazegame.ui

/**
 * Event listener for reacting to maze cell selection.
 */
fun interface MazeCellSelectionListener {
    fun onMazeCellSelected(x: Int, y: Int)
}

/**
 * Event listener for reacting to player selection.
 */
interface PlayerSelectionListener {
    fun onPlayerSelected(player: UiPlayerInformation)
    fun onPlayerSelectionCleared()
}
