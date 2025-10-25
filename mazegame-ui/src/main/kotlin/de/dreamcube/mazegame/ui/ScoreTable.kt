package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn

class ScoreTable(internal val controller: UiController) : JTable(), PlayerConnectionListener {

    val scoreFont: Font = Font(font.name, Font.PLAIN, 16)

    val cellRenderer: DefaultTableCellRenderer
    val uiPlayerInformationModel: UiPlayerCollection

    init {
        columnSelectionAllowed = false
        rowSelectionAllowed = true
        rowHeight += 4
        intercellSpacing = Dimension(0, 3)


        cellRenderer = object : DefaultTableCellRenderer() {
            private var col: Int = -1
            private var row: Int = -1

            override fun getTableCellRendererComponent(
                table: JTable?,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component? {
                col = column
                this.row = row
                val default = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

                default.font = if (col == 1 || col == 2) scoreFont.deriveFont(Font.BOLD) else scoreFont

                return default
            }

            override fun setValue(value: Any?) {
                if (value is String) {
                    // colors
                    val playerColor = uiPlayerInformationModel[row]?.color
                    val playerBgColor = uiPlayerInformationModel[row]?.bgColor

                    selectionForeground = playerBgColor ?: selectionForeground
                    foreground = if (row == selectedRow) selectionForeground else playerColor ?: foreground

                    selectionBackground = playerColor ?: selectionBackground
                    background = if (row == selectedRow) selectionBackground else playerBgColor

                    horizontalAlignment = when (col) {
                        0 -> CENTER // ID
                        1 -> LEFT // Nick
                        else -> RIGHT // All numbers for better comparison
                    }
                    setText(value)
                } else {
                    super.setValue(value)
                }
            }
        }

        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        selectionModel.addListSelectionListener(this)

        uiPlayerInformationModel = UiPlayerCollection(controller)
        model = uiPlayerInformationModel

        for ((index, columnHeader) in UiPlayerCollection.COLUMN_NAMES.withIndex()) {
            val currentColumn: TableColumn = getColumn(columnHeader)
            currentColumn.preferredWidth =
                when (index) {
                    0 -> 25
                    1 -> 150
                    2 -> 80
                    3 -> 70
                    4 -> 75
                    else -> 1
                }
            currentColumn.cellRenderer = cellRenderer
        }
        adjustPreferredViewportSize()
        controller.prepareEventListener(this)
    }

    internal fun reset() {
        uiPlayerInformationModel.reset()
    }

    private fun adjustPreferredViewportSize() {
        preferredScrollableViewportSize = Dimension(90 * UiPlayerCollection.COLUMN_NAMES.size, rowHeight * model.rowCount)
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        super.valueChanged(e)
        if (selectedRow >= 0) {
            val selectedPlayer: UiPlayerInformation? = uiPlayerInformationModel[selectedRow]
            if (selectedPlayer != null) {
                controller.glassPane.markPlayer(selectedPlayer)
            } else {
                controller.glassPane.clearMark()
            }
        } else {
            controller.glassPane.clearMark()
        }
    }

    override fun onPlayerLogin(playerSnapshot: PlayerSnapshot) {
        controller.uiScope.launch {
            adjustPreferredViewportSize()
        }
    }

    override fun onOwnPlayerLogin(playerSnapshot: PlayerSnapshot) {
        // ignore
    }

    override fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        controller.uiScope.launch {
            adjustPreferredViewportSize()
        }
    }
}