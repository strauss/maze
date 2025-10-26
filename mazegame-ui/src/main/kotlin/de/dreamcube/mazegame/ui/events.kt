package de.dreamcube.mazegame.ui

fun interface MazeCellSelectionListener {
    fun onMazeCellSelected(x: Int, y: Int)
}