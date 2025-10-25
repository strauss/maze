package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.client.maze.events.ScoreChangeListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import javax.swing.table.AbstractTableModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class UiPlayerInformation(
    initialPlayerSnapshot: PlayerSnapshot,
    initialColor: Color = Color.darkGray
) {

    companion object {
        private val BLACKISH: Color = Color(Color.HSBtoRGB(0.0f, 0.0f, 0.1f))
        private val WHITEISH: Color = Color(Color.HSBtoRGB(0.0f, 0.0f, 0.9f))
    }

    var snapshot: PlayerSnapshot = initialPlayerSnapshot
        internal set
    var color: Color = initialColor
        internal set
    var bgColor: Color = determineBgColorByLuminance(color)

    val id
        get() = snapshot.id

    private fun determineBgColorByLuminance(color: Color): Color {
        /**
         * Converts a normalized RBG component to "linear"
         */
        fun lin(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        fun contrast(lum1: Double, lum2: Double) = (max(lum1, lum2) + 0.05) / (min(lum1, lum2) + 0.05)

        val linRed: Double = lin(color.red / 255.0)
        val linGreen: Double = lin(color.green / 255.0)
        val linBlue: Double = lin(color.blue / 255.0)

        val luminance = 0.2126 * linRed + 0.7152 * linGreen + 0.0722 * linBlue
        val contrastToBlack = contrast(luminance, 0.1)
        val contrastToWhite = contrast(luminance, 0.9)

        return if (contrastToBlack > contrastToWhite) BLACKISH else WHITEISH
    }

}

enum class ScoreRepresentationMode {
    /**
     * In this mode, the scores from the server are represented "as is".
     */
    SERVER,

    /**
     * In this mode, the score counting starts at 0 at connection time, meaning the first reported score at login of each other player is subtracted
     * as offset in the representation.
     */
    CLIENT;

    fun toggle() = when (this) {
        SERVER -> CLIENT
        CLIENT -> SERVER
    }
}

/**
 * This collection serves as [AbstractTableModel] for the score view. It also serves as data structure for maintaining client-side player information.
 */
class UiPlayerCollection(private val controller: UiController) : AbstractTableModel(), PlayerConnectionListener, ScoreChangeListener {

    companion object {
        val COLUMN_NAMES: List<String> = listOf("ID", "Nick", "Score", "ms/Step", "Pts/min")
        private val LOGGER: Logger = LoggerFactory.getLogger(UiPlayerCollection::class.java)
    }

    init {
        controller.uiPlayerCollection = this
        controller.prepareEventListener(this)
    }

    private val idToIndexMap: MutableMap<Int, Int> = HashMap()
    private val uiPlayerInformationList: MutableList<UiPlayerInformation> = ArrayList()

    private var scoreRepresentationMode = ScoreRepresentationMode.SERVER

    operator fun get(index: Int): UiPlayerInformation? {
        if (index < 0 || index >= uiPlayerInformationList.size) {
            return null
        }
        return uiPlayerInformationList[index]
    }

    fun getById(id: Int): UiPlayerInformation? = idToIndexMap[id]?.let /*us*/ { get(it) }

    /**
     * If the player represented by the [snapshot] is not included yet, it is added to the collection. Otherwise, it is just updated. The function
     * should be called, whenever a new player joins or a player's score changes.
     */
    internal fun addOrUpdate(snapshot: PlayerSnapshot) {
        val currentIndex: Int? = idToIndexMap[snapshot.id]
        if (currentIndex == null) {
            val newValue = UiPlayerInformation(snapshot)
            val newIndex = idToIndexMap.size
            idToIndexMap[snapshot.id] = newIndex
            uiPlayerInformationList.add(newValue)
            redistributePlayerColorsByRainbow()
            fireTableRowsUpdated(0, newIndex - 1)
            fireTableRowsInserted(newIndex, newIndex)
        } else {
            val currentValue: UiPlayerInformation = uiPlayerInformationList[currentIndex]
            currentValue.snapshot = snapshot
            fireTableRowsUpdated(currentIndex, currentIndex)
        }
    }

    /**
     * Removes the player represented by the [snapshot] from the collection.
     */
    internal fun remove(snapshot: PlayerSnapshot) {
        val currentIndex: Int? = idToIndexMap[snapshot.id]
        if (currentIndex == null) {
            return
        }
        val removed: UiPlayerInformation = uiPlayerInformationList.removeAt(currentIndex)
        if (removed.id != snapshot.id) {
            LOGGER.error("Removed ids don't match!")
        }
        fireTableRowsDeleted(currentIndex, currentIndex)
        // rehash the indexes
        idToIndexMap.remove(snapshot.id)
        var index = 0
        for (uiPlayerInformation in uiPlayerInformationList) {
            idToIndexMap[uiPlayerInformation.id] = index
            index += 1
        }
    }

    internal fun toggleScoreRepresentation() {
        scoreRepresentationMode = scoreRepresentationMode.toggle()
        fireTableDataChanged()
    }

    internal fun reset() {
        uiPlayerInformationList.clear()
        idToIndexMap.clear()
        scoreRepresentationMode = ScoreRepresentationMode.SERVER
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = uiPlayerInformationList.size

    override fun getColumnCount(): Int = COLUMN_NAMES.size

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (rowIndex >= rowCount || columnIndex >= columnCount) {
            return null
        }
        val valueAt: UiPlayerInformation = uiPlayerInformationList[rowIndex]
        return when (columnIndex) {
            // ID
            0 -> valueAt.id.toString()

            // Nick/Alias
            1 -> valueAt.snapshot.nick

            // Score
            2 -> when (scoreRepresentationMode) {
                ScoreRepresentationMode.SERVER -> valueAt.snapshot.score.toString()
                ScoreRepresentationMode.CLIENT -> valueAt.snapshot.localScore.toString()
            }

            // ms/step
            3 -> valueAt.snapshot.moveTime.toString()

            // ppm
            4 -> valueAt.snapshot.pointsPerMinute.toString()

            else -> valueAt
        }
    }

    /**
     * We currently only display [String]s in the table.
     */
    override fun getColumnClass(columnIndex: Int): Class<*>? {
        if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.size) {
            return null
        }
        return String::class.java
    }

    /**
     * No cell is editable. They are all read-only
     */
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

    override fun getColumnName(columnIndex: Int): String? {
        if (columnIndex < 0 || columnIndex >= COLUMN_NAMES.size) {
            return null
        }
        return COLUMN_NAMES[columnIndex]
    }

    override fun onPlayerLogin(playerSnapshot: PlayerSnapshot) {
        addOrUpdate(playerSnapshot)
    }

    override fun onOwnPlayerLogin(playerSnapshot: PlayerSnapshot) {
        // ignore
    }

    override fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        remove(playerSnapshot)
    }

    override fun onScoreChange(oldScore: Int, newPlayerSnapshot: PlayerSnapshot) {
        addOrUpdate(newPlayerSnapshot)
    }

    internal fun redistributePlayerColorsByRainbow() {
        // assign new colors
        val playerCount: Int = rowCount
        var hue = 0.485f
        val maxHue = 1.360f
        val maxB = 0.9f
        val minB = 0.7f
        var deltaHue = maxHue - hue
        if (playerCount > 1) {
            deltaHue /= (playerCount - 1).toFloat()
        }
        for (playerInformation: UiPlayerInformation in uiPlayerInformationList) {
            val desiredColor = Color(Color.HSBtoRGB(hue, 1.0f, maxB))
            val green: Int = desiredColor.green
            val relativeGreen: Float = green / 255f
            val brightness: Float = maxB - (maxB - minB) * relativeGreen
            val actualColor = Color(Color.HSBtoRGB(hue, 1.0f, brightness))
            playerInformation.color = actualColor
            hue += deltaHue
        }
    }
}