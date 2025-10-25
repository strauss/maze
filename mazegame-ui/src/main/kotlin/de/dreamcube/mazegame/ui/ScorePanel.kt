package de.dreamcube.mazegame.ui

import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane

class ScorePanel(val scoreTable: ScoreTable) : JPanel() {

    init {
        layout = BorderLayout()
        val tableScrollPane = JScrollPane(scoreTable)
        add(tableScrollPane, BorderLayout.CENTER)

        val toggleButton = JButton("Toggle Score")
        add(toggleButton, BorderLayout.SOUTH)

        toggleButton.addActionListener {
            scoreTable.controller.uiPlayerCollection.toggleScoreRepresentation()
        }
    }

}