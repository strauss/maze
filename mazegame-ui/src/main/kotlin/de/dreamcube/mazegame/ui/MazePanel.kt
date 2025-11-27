package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.ui.MazePanel.Companion.MAX_ZOOM
import de.dreamcube.mazegame.ui.MazePanel.Companion.MIN_ZOOM
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.event.*
import javax.swing.JPanel
import kotlin.math.min

/**
 * Maze visualization. Renders a [java.awt.image.BufferedImage] and places it at the [offset] point. Keeps the image
 * up to date using the [MazeModel].
 */
class MazePanel() : JPanel() {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MazePanel::class.java)
        internal const val INITIAL_ZOOM = 17
        private const val MIN_ZOOM = 5
        private const val MAX_ZOOM = 33
    }

    /**
     * The current zoom factor. The setter only allows updates if they are in the allowed range between [MIN_ZOOM] and
     * [MAX_ZOOM].
     */
    var zoom: Int = INITIAL_ZOOM
        private set(value) {
            if (value in MIN_ZOOM..MAX_ZOOM) {
                field = value
                UiController.markerPane.repaint()
                UiController.updateZoom(value)
            }
        }

    /**
     * The maze image.
     */
    private lateinit var image: Image

    /**
     * The zoom factor of the image. If the [imageZoom] differs from the actual [zoom], the whole image is re-rendered.
     */
    var imageZoom = 0
        private set

    /**
     * The current offset point.
     */
    internal var offset: Point = Point(0, 0)
        private set

    /**
     * A point used for relocating the offset while one of the allowed mouse buttons is pressed.
     */
    private var pressPoint: Point? = null

    /**
     * Resets the maze visualization.
     */
    internal fun reset() {
        // Erase the image with a transparent rectangle
        image.graphics.color = Color(0xff, 0xff, 0xff, 0x00)
        image.graphics.drawRect(0, 0, width * zoom, height * zoom)
        repaint()

        // change all values
        zoom = INITIAL_ZOOM
        imageZoom = 0
        offset.x = 0
        offset.y = 0
        UiController.updateOffset(offset.x, offset.y)
        pressPoint = null
        UiController.markerPane.reset()
    }

    init {
        UiController.mazePanel = this
        minimumSize = Dimension(12 * zoom, 9 * zoom)
        preferredSize = Dimension(45 * zoom, 35 * zoom)

        /**
         * Anonymous [MouseListener] for reacting certain [MouseEvent]s.
         */
        val mouseListener: MouseListener = object : MouseAdapter() {

            /**
             * Reacts to certain mouse clicks.
             */
            override fun mouseClicked(e: MouseEvent?) {
                // on double-click we center the maze
                if (UiController.mazeModel.mazeReceived && e?.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    centerMaze()
                    return
                }
                // when clicking on a selectable maze cell, we fire the matching event
                if (UiController.mazeModel.mazeReceived && e?.button == MouseEvent.BUTTON1 && e.clickCount == 1) {
                    val x = (e.x - offset.x) / zoom
                    val y = (e.y - offset.y) / zoom
                    if (x >= 0 && y >= 0 && x < UiController.mazeModel.width && y < UiController.mazeModel.height) {
                        UiController.fireMazeCellSelected(x, y, UiController.mazeModel[x, y])
                    }
                }
            }

            /**
             * For dragging the maze, we store the source point, where we started.
             */
            override fun mousePressed(e: MouseEvent?) {
                if (e != null && e.button != MouseEvent.BUTTON1) {
                    pressPoint = e.point
                }
            }

            /**
             * When releasing the mouse, we delete the point.
             */
            override fun mouseReleased(e: MouseEvent?) {
                pressPoint = null
            }
        }
        val mouseMotionListener: MouseMotionListener = object : MouseMotionAdapter() {
            /**
             * The real drag action begins here :-D
             */
            override fun mouseDragged(e: MouseEvent?) {
                if (e != null && UiController.mazeModel.mazeReceived && pressPoint != null) {
                    offset.x += e.getX() - pressPoint!!.x
                    offset.y += e.getY() - pressPoint!!.y
                    pressPoint = e.getPoint()

                    offset.x = min(offset.x, getWidth() - 10 * zoom)
                        .coerceAtLeast(-(UiController.mazeModel.width - 10) * zoom)
                    offset.y = min(offset.y, getHeight() - 10 * zoom)
                        .coerceAtLeast(-(UiController.mazeModel.height - 10) * zoom)
                    repaint()
                    UiController.updateOffset(offset.x, offset.y)
                }
            }

            /**
             * This reacts to events when the mouse is moved over the maze.
             */
            override fun mouseMoved(e: MouseEvent?) {
                val mx = ((e?.x ?: 0) - offset.x) / zoom
                val my = ((e?.y ?: 0) - offset.y) / zoom

                // if there is no marked player, we display the cell coordinates under the mouse on the status bar
                if (UiController.markerPane.playerToMark == null) {
                    UiController.updatePositionStatus(mx, my)
                }

                // if we are above a maze cell, we fire a hover event
                if (UiController.mazeModel.mazeReceived && mx in 0..<UiController.mazeModel.width && my in 0..<UiController.mazeModel.height) {
                    UiController.fireMazeCellHovered(mx, my, UiController.mazeModel[mx, my])
                }
            }
        }
        addMouseListener(mouseListener)
        addMouseMotionListener(mouseMotionListener)


        // Change zoom with mouse wheel ... we don't need no dialog shenanigans :-)
        addMouseWheelListener { e ->
            if (UiController.mazeModel.mazeReceived) {
                val rotation = e?.wheelRotation ?: 0
                if (rotation != 0) {
                    zoom -= rotation * 2 // stick to odd numbers
                }
            }
        }
    }

    override fun paint(g: Graphics) {
        if (UiController.mazeModel.mazeReceived) {
            super.paint(g)
            // paining involves a potential re-rendering of the image...
            checkImage()
            // ... followed by actually drawing the image at the offset location
            g.drawImage(image, offset.x, offset.y, this)
        }
    }

    /**
     * Checks if the image requires re-rendering and does so if it is required. The maze is also centered whenever this
     * happens.
     */
    internal fun checkImage() {
        if (UiController.mazeModel.mazeReceived && !this::image.isInitialized || imageZoom != zoom) {
            imageZoom = zoom
            image = createImage(UiController.mazeModel.width * zoom, UiController.mazeModel.height * zoom)
            centerMaze()
            paintCompleteMaze(image.graphics)
        }
    }

    /**
     * Paints the whole maze.
     */
    private fun paintCompleteMaze(g: Graphics) {
        val maze: MazeModel = UiController.mazeModel
        for (y in 0..<maze.height) {
            for (x in 0..<maze.width) {
                val mazeField: MazeModel.MazeField = maze[x, y]
                internalUpdatePosition(g, x, y, mazeField)
            }
        }
    }

    /**
     * Updates a single position. This optimizes render performance if the maze is not resized.
     */
    fun updatePosition(x: Int, y: Int) {
        checkImage()
        val g: Graphics = image.graphics
        val mazeField: MazeModel.MazeField = UiController.mazeModel[x, y]
        internalUpdatePosition(g, x, y, mazeField)
    }

    /**
     * Draws a maze cell according to its content. The actual drawing is done in the [DrawableMazeObject].
     */
    private fun internalUpdatePosition(g: Graphics, x: Int, y: Int, mazeField: MazeModel.MazeField) {
        when (mazeField) {
            is MazeModel.PathMazeField -> drawPath(g, x, y, mazeField)
            is MazeModel.WallMazeField -> DrawableWall.drawAt(g, x, y, zoom)
            is MazeModel.OutsideMazeField -> DrawableOutside.drawAt(g, x, y, zoom)
            is MazeModel.UnknownMazeField -> DrawableUnknown.drawAt(g, x, y, zoom)
        }
    }

    /**
     * Draws a path cell and its potential content.
     */
    private fun drawPath(g: Graphics, x: Int, y: Int, mazeField: MazeModel.PathMazeField) {
        // always draw the path, no matter what is on it
        DrawablePath.drawAt(g, x, y, zoom)
        when (mazeField.occupationStatus) {
            MazeModel.PathOccupationStatus.EMPTY -> {
                // we already drew the path, so we skip here
            }

            MazeModel.PathOccupationStatus.BAIT -> drawBait(mazeField, g, x, y)
            MazeModel.PathOccupationStatus.PLAYER -> drawPlayer(mazeField, g, x, y)
            MazeModel.PathOccupationStatus.CROWDED -> {
                LOGGER.warn("Crowded field detected at ($x : $y).")
                drawBait(mazeField, g, x, y)
                drawPlayer(mazeField, g, x, y)
            }
        }
    }

    /**
     * Draws a player.
     */
    private fun drawPlayer(mazeField: MazeModel.PathMazeField, g: Graphics, x: Int, y: Int) {
        val player: PlayerSnapshot? = mazeField.player
        player?.let { p ->
            val playerColor: Color? = UiController.uiPlayerCollection.getById(p.id)?.color
            playerColor?.let { color ->
                DrawablePlayer(color, p.viewDirection).drawAt(g, x, y, zoom)
            }
        }
    }

    /**
     * Draws a bait.
     */
    private fun drawBait(mazeField: MazeModel.PathMazeField, g: Graphics, x: Int, y: Int) {
        mazeField.bait?.drawable()?.drawAt(g, x, y, zoom)
    }

    /**
     * Centers the maze using highly advanced black magic :-)
     */
    private fun centerMaze() {
        offset.x = ((getWidth() - UiController.mazeModel.width * zoom) / 2)
            .coerceAtMost(getWidth() - 10 * zoom)
            .coerceAtLeast(-(UiController.mazeModel.width - 10) * zoom)
        offset.y = ((getHeight() - UiController.mazeModel.height * zoom) / 2)
            .coerceAtMost(getHeight() - 10 * zoom)
            .coerceAtLeast(-(UiController.mazeModel.height - 10) * zoom)
        repaint()
        UiController.updateOffset(offset.x, offset.y)
    }

}