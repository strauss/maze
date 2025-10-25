package de.dreamcube.mazegame.ui

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane

class ScorePanel(val scoreTable: ScoreTable) : JPanel() {

    init {
        layout = BorderLayout()
        val tableScrollPane = JScrollPane(scoreTable)
        add(tableScrollPane, BorderLayout.CENTER)

        val anotherPanel = JPanel()
        anotherPanel.layout = BorderLayout()
        val toggleButton = JButton("Toggle Score")
        anotherPanel.add(toggleButton, BorderLayout.NORTH)

        val dummyPanel = JPanel()
        dummyPanel.preferredSize = Dimension(50, 20)
        anotherPanel.add(dummyPanel, BorderLayout.CENTER)

        add(anotherPanel, BorderLayout.SOUTH)

        toggleButton.addActionListener {
            scoreTable.controller.uiPlayerCollection.toggleScoreRepresentation()
        }
    }

}