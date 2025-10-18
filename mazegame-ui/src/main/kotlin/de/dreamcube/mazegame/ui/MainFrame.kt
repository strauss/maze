package de.dreamcube.mazegame.ui

import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane

class MainFrame(val controller: UiController) : JFrame(TITLE) {
    companion object {
        private const val TITLE = "Maze-Game Client KT"
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        // Fill the UI
        contentPane.layout = BorderLayout()
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        val connectionSettingsPanel = ConnectionSettingsPanel(controller)
        splitPane.add(connectionSettingsPanel, JSplitPane.LEFT)
        val emptyPanel = JPanel()
        emptyPanel.setSize(100, 200)
        splitPane.add(emptyPanel, JSplitPane.RIGHT)
        contentPane.add(splitPane, BorderLayout.CENTER)


        // Size and position
        setSize(640, 480)
        isLocationByPlatform = true
        isVisible = true
    }
}