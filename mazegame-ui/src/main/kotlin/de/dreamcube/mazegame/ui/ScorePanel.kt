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

import net.miginfocom.swing.MigLayout
import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.*

/**
 * Contains the score table, the score toggle button and the color options.
 */
class ScorePanel() : JPanel() {

    private val scoreTable: ScoreTable
        get() = UiController.scoreTable

    var colorOptionsVisible = false

    init {
        layout = BorderLayout()
        val tableScrollPane = JScrollPane(scoreTable)
        add(tableScrollPane, BorderLayout.CENTER)

        val belowTablePanel = JPanel()
        belowTablePanel.layout = BorderLayout()

        val toggleButton = JButton("Toggle Score")
        belowTablePanel.add(toggleButton, BorderLayout.NORTH)

        val dummyPanel = JPanel()
        dummyPanel.preferredSize = Dimension(50, 20)
        belowTablePanel.add(dummyPanel, BorderLayout.CENTER)

        val colorOptionsPanel = ColorOptionPanel()
        colorOptionsPanel.preferredSize = Dimension(300, 300)

        val colorButton = JButton("Show color options")
        belowTablePanel.add(colorButton, BorderLayout.SOUTH)
        colorButton.addActionListener { _ ->
            if (colorOptionsVisible) {
                belowTablePanel.remove(colorOptionsPanel)
                belowTablePanel.add(dummyPanel, BorderLayout.CENTER)
                colorButton.text = "Show color options"
                colorOptionsVisible = false
            } else {
                belowTablePanel.remove(dummyPanel)
                belowTablePanel.add(colorOptionsPanel, BorderLayout.CENTER)
                colorButton.text = "Hide color options"
                colorOptionsVisible = true
            }
            revalidate()
            repaint()
        }

        add(belowTablePanel, BorderLayout.SOUTH)

        toggleButton.addActionListener {
            UiController.uiPlayerCollection.toggleScoreRepresentation()
        }

    }

    /**
     * The color options panel.
     */
    private class ColorOptionPanel : JPanel() {
        init {
            layout = BorderLayout()

            val colorCircleComponent = object : JComponent() {
                override fun paint(g: Graphics?) {
                    val g2 = g as Graphics2D
                    val x = 0.0
                    val y = 0.0
                    val w = 250
                    val h = 250
                    val centerX = x + w / 4
                    val centerY = y + h / 4
                    val widthInner = w.toDouble() / 2.0
                    val heightInner = w.toDouble() / 2.0

                    val hiResImage = renderHiRes(w, h, 4) { g ->
                        g.run {
                            val circle = Ellipse2D.Double(centerX, centerY, widthInner, heightInner)
                            val circleArea = Area(circle)
                            var deg = 0.0
                            val startingColorDegree = UiController.uiPlayerCollection.startingColorDegree
                            for (colorDegree: Int in 0..<360) {
                                val actualColorDegree = (colorDegree + startingColorDegree) % 360
                                val currentColorSegment =
                                    UiController.uiPlayerCollection.colorDistribution[actualColorDegree]
                                if (currentColorSegment.active) {
                                    val arc = Arc2D.Double(x, y, w.toDouble(), h.toDouble(), deg, 1.0, Arc2D.PIE)
                                    val ringSeg: Area = Area(arc).apply { subtract(circleArea) }
                                    color = currentColorSegment.c
                                    g.fill(ringSeg)
                                }
                                deg -= 1.0
                            }
                            // draw a gray line to indicate 0 degree.
                            color = Color(0, 0, 128)
                            stroke = BasicStroke(2f)
                            drawLine(w / 2, h / 2, w, h / 2)
                        }
                    }

                    // downscale the image for reducing moiré effect
                    g2.renderingHints[RenderingHints.KEY_INTERPOLATION] = RenderingHints.VALUE_INTERPOLATION_BICUBIC
                    g2.drawImage(hiResImage, 25, 25, w, h, null)
                }
            }

            add(colorCircleComponent, BorderLayout.CENTER)

            val colorWeaknessPanel = JPanel()
            colorWeaknessPanel.layout = MigLayout("wrap 1")

            val headerLabel = JLabel("Color vision deficiency options")
            headerLabel.font = headerLabel.font.deriveFont(Font.BOLD)
            colorWeaknessPanel.add(headerLabel)

            // radio buttons for color options (color deficiency mode)
            val normalButton = JRadioButton("Normal")
            val protanButton = JRadioButton("Protan (red-weak)")
            val deutanButton = JRadioButton("Deutan (green-weak)")
            val tritanButton = JRadioButton("Tritan (blue-weak)")
            ButtonGroup().apply {
                add(normalButton)
                add(protanButton)
                add(deutanButton)
                add(tritanButton)
            }
            colorWeaknessPanel.add(normalButton)
            colorWeaknessPanel.add(protanButton)
            colorWeaknessPanel.add(deutanButton)
            colorWeaknessPanel.add(tritanButton)

            normalButton.addChangeListener { _ ->
                if (normalButton.isSelected) {
                    UiController.uiPlayerCollection.configureForNormal()
                    colorCircleComponent.repaint()
                }
            }

            protanButton.addChangeListener { _ ->
                if (protanButton.isSelected) {
                    UiController.uiPlayerCollection.configureForProtan()
                    colorCircleComponent.repaint()
                }
            }

            deutanButton.addChangeListener { _ ->
                if (deutanButton.isSelected) {
                    UiController.uiPlayerCollection.configureForDeutan()
                    colorCircleComponent.repaint()
                }
            }

            tritanButton.addChangeListener { _ ->
                if (tritanButton.isSelected) {
                    UiController.uiPlayerCollection.configureForTritan()
                    colorCircleComponent.repaint()
                }
            }
            normalButton.isSelected = true

            // Label and button for manual color distribution
            val redistributionLabel = JLabel("Manual Color redistribution")
            redistributionLabel.font = redistributionLabel.font.deriveFont(Font.BOLD)
            colorWeaknessPanel.add(redistributionLabel)

            val redistributionButton = JButton("Redistribute player colors")
            redistributionButton.addActionListener { _ ->
                UiController.uiPlayerCollection.redistributePlayerColorsByDistribution()
            }

            colorWeaknessPanel.add(redistributionButton)

            add(colorWeaknessPanel, BorderLayout.EAST)

            // Slider for selecting the start hue at 0 degree
            val turnSlider = JSlider(SwingConstants.VERTICAL).apply {
                minimum = 0
                maximum = 360
                value = UiController.uiPlayerCollection.startingColorDegree
                majorTickSpacing = 90
                minorTickSpacing = 15
                paintLabels = true
                snapToTicks = true
                paintTicks = true
                addChangeListener { _ ->
                    UiController.uiPlayerCollection.startingColorDegree = value % 360
                    UiController.uiPlayerCollection.redistributePlayerColorsByDistribution()
                    colorCircleComponent.repaint()
                }
            }

            add(turnSlider, BorderLayout.WEST)

            isVisible = true
        }

        private fun renderHiRes(w: Int, h: Int, scale: Int, paintContent: (Graphics2D) -> Unit): BufferedImage {
            val hi = BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_ARGB)
            val g = hi.createGraphics()
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

                // scale up
                g.scale(scale.toDouble(), scale.toDouble())

                // Everything is transparent
                g.composite = AlphaComposite.Src
                g.color = Color(0, 0, 0, 0)
                g.fillRect(0, 0, w, h)
                g.composite = AlphaComposite.SrcOver

                paintContent(g)
            } finally {
                g.dispose()
            }
            return hi
        }
    }

}