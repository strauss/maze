/*
 * Maze Game
 * Copyright (c) 2025 Sascha Strauß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import de.dreamcube.mazegame.client.maze.events.ScoreChangeListener
import de.dreamcube.mazegame.ui.UiPlayerInformation.Companion.MARKER_ALPHA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import javax.swing.table.AbstractTableModel
import kotlin.math.max

/**
 * Enriches the [snapshot] of a player with information that is required for rendering the player object, mainly the
 * color(s).
 */
class UiPlayerInformation(
    initialPlayerSnapshot: PlayerSnapshot,
    initialColor: Color = Color.darkGray
) {

    companion object {
        private const val MARKER_ALPHA = 192
    }

    /**
     * The [snapshot] of the player.
     */
    var snapshot: PlayerSnapshot = initialPlayerSnapshot
        internal set

    /**
     * The player's color. The setter also determines the [markerColor] (more transparent) and the [bgColor] in the
     * score table.
     */
    var color: Color = initialColor
        internal set(value) {
            field = value
            bgColor = determineBgColorByLuminance(value)
            markerColor = field.changeAlpha(MARKER_ALPHA)
        }

    /**
     * The background color in the [ScoreTable]. See [determineBgColorByLuminance] on how it is determined.
     */
    var bgColor: Color = determineBgColorByLuminance(initialColor)

    /**
     * The color that is used for the marker if the player is selected. It is derived from the [color] but with
     * [MARKER_ALPHA] as alpha value.
     */
    var markerColor: Color = initialColor.changeAlpha(MARKER_ALPHA)

    val id
        get() = snapshot.id

    private fun Color.changeAlpha(alpha: Int) = Color(this.red, this.green, this.blue, alpha)

}

/**
 * Enum for the score representation.
 */
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
 * This collection serves as [AbstractTableModel] for the score view. It also serves as data structure for maintaining
 * client-side player information.
 */
class UiPlayerCollection() : AbstractTableModel(), PlayerConnectionListener, ScoreChangeListener {

    companion object {
        val COLUMN_NAMES: List<String> = listOf("ID", "Nick", "Score", "ms/Step", "Pts/min")
        private val LOGGER: Logger = LoggerFactory.getLogger(UiPlayerCollection::class.java)
    }

    private val idToIndexMap: MutableMap<Int, Int> = HashMap()
    private val uiPlayerInformationList: MutableList<UiPlayerInformation> = ArrayList()

    internal val colorDistribution: List<ColorSegment> = getColorDistribution(false)
    var startingColorDegree = 180
        internal set

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
     * If the player represented by the [snapshot] is not included yet, it is added to the collection. Otherwise, it is
     * just updated. The function should be called, whenever a new player joins or a player's score changes.
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

    internal fun clearScoresLocally() {
        UiController.bgScope.launch {
            UiController.client.softResetScores()
            uiPlayerInformationList.forEach {
                // quick update of all players
                it.snapshot = it.snapshot.view.takeSnapshot()
            }
            withContext(Dispatchers.Swing) {
                scoreRepresentationMode = ScoreRepresentationMode.CLIENT
                fireTableDataChanged()
            }
        }
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

    override fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        remove(playerSnapshot)
    }

    override fun onScoreChange(oldScore: Int, newPlayerSnapshot: PlayerSnapshot) {
        addOrUpdate(newPlayerSnapshot)
    }

    internal fun redistributePlayerColorsByDistribution() {
        if (uiPlayerInformationList.isEmpty()) {
            return
        }
        val usableColors: Int = colorDistribution.asSequence().filter { it.active }.count()
        val delta: Int = max(1, usableColors / uiPlayerInformationList.size)
        var colorIndex = startingColorDegree
        val colorDistributionMap: Map<Int, Color> = buildMap {
            for (playerInformation: UiPlayerInformation in uiPlayerInformationList) {
                while (!colorDistribution[colorIndex].active) {
                    colorIndex += 1
                    colorIndex %= 360
                }
                playerInformation.color = colorDistribution[colorIndex].c
                put(playerInformation.id, playerInformation.color)
                colorIndex += delta
                colorIndex %= 360
            }
        }
        UiController.colorDistributionChanged(colorDistributionMap)
        UiController.scoreTable.repaint()
    }

    /**
     * Re-enables all hues.
     */
    private fun configureFullSpectrum() {
        colorDistribution.forEach { it.activeByConfiguration = true }
    }

    /**
     * Allows all color hues.
     */
    internal fun configureForNormal() {
        configureFullSpectrum()
        redistributePlayerColorsByDistribution()
    }

    /**
     * Disables certain hues for protan mode.
     */
    internal fun configureForProtan() {
        configureFullSpectrum()
        for (degree in 350..<360) {
            colorDistribution[degree].activeByConfiguration = false
        }
        for (degree in 0..40) {
            colorDistribution[degree].activeByConfiguration = false
        }
        for (degree in 95..150) {
            colorDistribution[degree].activeByConfiguration = false
        }
        redistributePlayerColorsByDistribution()
    }

    /**
     * Disables certain hues for deutan mode.
     */
    internal fun configureForDeutan() {
        configureFullSpectrum()
        for (degree in 350..<360) {
            colorDistribution[degree].activeByConfiguration = false
        }
        for (degree in 0..40) {
            colorDistribution[degree].activeByConfiguration = false
        }
        for (degree in 100..145) {
            colorDistribution[degree].activeByConfiguration = false
        }
        redistributePlayerColorsByDistribution()
    }

    /**
     * Disables certain hues for tritan mode.
     */
    internal fun configureForTritan() {
        configureFullSpectrum()
        for (degree in 50..75) {
            colorDistribution[degree].activeByConfiguration = false
        }
        for (degree in 185..255) {
            colorDistribution[degree].activeByConfiguration = false
        }
        redistributePlayerColorsByDistribution()
    }
}