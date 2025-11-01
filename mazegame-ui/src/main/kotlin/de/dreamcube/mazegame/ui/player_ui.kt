package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.client.maze.events.ScoreChangeListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import javax.swing.table.AbstractTableModel

class UiPlayerInformation(
    initialPlayerSnapshot: PlayerSnapshot,
    initialColor: Color = Color.darkGray
) {

    companion object {
        private const val MARKER_ALPHA = 192
    }

    var snapshot: PlayerSnapshot = initialPlayerSnapshot
        internal set
    var color: Color = initialColor
        internal set(value) {
            field = value
            bgColor = determineBgColorByLuminance(value)
            markerColor = field.changeAlpha(MARKER_ALPHA)
        }
    var bgColor: Color = determineBgColorByLuminance(initialColor)
    var markerColor: Color = initialColor.changeAlpha(MARKER_ALPHA)

    val id
        get() = snapshot.id

    private fun Color.changeAlpha(alpha: Int) = Color(this.red, this.green, this.blue, alpha)

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
class UiPlayerCollection() : AbstractTableModel(), PlayerConnectionListener, ScoreChangeListener {

    companion object {
        val COLUMN_NAMES: List<String> = listOf("ID", "Nick", "Score", "ms/Step", "Pts/min")
        private val LOGGER: Logger = LoggerFactory.getLogger(UiPlayerCollection::class.java)
    }

    private val idToIndexMap: MutableMap<Int, Int> = HashMap()
    private val uiPlayerInformationList: MutableList<UiPlayerInformation> = ArrayList()

    private lateinit var colorDistribution: List<Color>

    private var scoreRepresentationMode = ScoreRepresentationMode.SERVER

    init {
        UiController.uiPlayerCollection = this
        UiController.prepareEventListener(this)
    }

    operator fun get(index: Int): UiPlayerInformation? {
        if (index < 0 || index >= uiPlayerInformationList.size) {
            return null
        }
        return uiPlayerInformationList[index]
    }

    fun getById(id: Int): UiPlayerInformation? = getIndex(id)?.let /*us*/ { get(it) }

    /**
     * If the player represented by the [snapshot] is not included yet, it is added to the collection. Otherwise, it is just updated. The function
     * should be called, whenever a new player joins or a player's score changes.
     */
    internal fun addOrUpdate(snapshot: PlayerSnapshot) {
        val currentIndex: Int? = getIndex(snapshot.id)
        if (currentIndex == null) {
            val newValue = UiPlayerInformation(snapshot)
            val newIndex = idToIndexMap.size
            idToIndexMap[snapshot.id] = newIndex
            uiPlayerInformationList.add(newValue)
            redistributePlayerColorsByDistribution()
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
        val currentIndex: Int? = getIndex(snapshot.id)
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

    internal fun getIndex(id: Int): Int? = idToIndexMap[id]

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
            2 -> {
                val scoreToDisplay = when (scoreRepresentationMode) {
                    ScoreRepresentationMode.SERVER -> valueAt.snapshot.score
                    ScoreRepresentationMode.CLIENT -> valueAt.snapshot.localScore
                }
                String.format("%,d", scoreToDisplay)
            }

            // ms/step
            3 -> String.format("%.2f", valueAt.snapshot.moveTime)

            // ppm
            4 -> String.format("%.2f", valueAt.snapshot.pointsPerMinute)

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

    internal fun redistributePlayerColorsByDistribution() {
        if (!this::colorDistribution.isInitialized) {
            colorDistribution = getColorDistribution(false, 360, 180)
        }
        if (colorDistribution.size < uiPlayerInformationList.size) {
            colorDistribution = getColorDistribution(false, colorDistribution.size * 2, colorDistribution.size)
        }
        val delta: Int = colorDistribution.size / uiPlayerInformationList.size
        var colorIndex = 0
        val colorDistributionMap: Map<Int, Color> = buildMap {
            for (playerInformation: UiPlayerInformation in uiPlayerInformationList) {
                playerInformation.color = colorDistribution[colorIndex]
                colorIndex += delta
                put(playerInformation.id, playerInformation.color)
            }
        }
        UiController.colorDistributionChanged(colorDistributionMap)
    }
}