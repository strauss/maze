package de.dreamcube.mazegame.ui

/**
 * Event listener for reacting to maze cell mouse interactions.
 */
fun interface MazeCellListener {
    fun onMazeCellSelected(x: Int, y: Int, mazeField: MazeModel.MazeField)
    fun onMazeCellHovered(x: Int, y: Int, mazeField: MazeModel.MazeField) {
        // do nothing by default, but allow for functional interface
    }
}

/**
 * Event listener for reacting to player selection.
 */
interface PlayerSelectionListener {
    fun onPlayerSelected(player: UiPlayerInformation)
    fun onPlayerSelectionCleared()
}
