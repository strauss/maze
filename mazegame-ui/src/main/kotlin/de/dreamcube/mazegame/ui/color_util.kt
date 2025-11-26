package de.dreamcube.mazegame.ui

import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

val BLACKISH: Color = Color(Color.HSBtoRGB(0.0f, 0.0f, 0.05f))
val WHITISH: Color = Color(Color.HSBtoRGB(0.0f, 0.0f, 0.95f))
const val SATURATION: Float = 1.0f

/**
 * Converts a normalized RBG component to "linear".
 */
private fun lin(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

/**
 * Calculates the contrast between two luminance values.
 */
private fun contrast(lum1: Double, lum2: Double) = (max(lum1, lum2) + 0.05) / (min(lum1, lum2) + 0.05)

/**
 * Compares the luminance between the color and both black and white. Whichever "wins" determines the resulting background color.
 */
fun determineBgColorByLuminance(color: Color): Color {
    val linRed: Double = lin(color.red / 255.0)
    val linGreen: Double = lin(color.green / 255.0)
    val linBlue: Double = lin(color.blue / 255.0)

    val luminance = 0.2126 * linRed + 0.7152 * linGreen + 0.0722 * linBlue
    val contrastToBlack = contrast(luminance, 0.0)
    val contrastToWhite = contrast(luminance, 1.0)

    return if (contrastToBlack > contrastToWhite) BLACKISH else WHITISH
}

/**
 * Creates a list of up to [granularity] usable colors. If [dark] is set, the colors are picked to match a dark theme. The color wheel is separated
 * into [granularity] many fragments. For each fragment a color with full saturation is created. Its brightness is adjusted to fit into the theme.
 * If this is impossible (blue on dark is a problem), it is skipped.
 */
fun getColorDistribution(dark: Boolean = false): List<ColorSegment> {
    val granularity = 360
    // If we want dark mode, the color has to be brighter
    // If we want light mode, the color has to be darker
    val deltaBrightness: Float = if (dark) 0.01f else -0.01f
    val startAdjustingAt: Float = if (dark) 0.0f else 1.0f
    val desiredBgColor: Color = if (dark) BLACKISH else WHITISH
    return buildList {
        for (degree: Int in 0..<granularity) {
            val hue: Float = (1.0f / granularity) * degree
            var brightness: Float = startAdjustingAt
            adjust@ while (true) {
                val colorCandidate: Color = Color.getHSBColor(hue, SATURATION, brightness)
                val resultingBgColor: Color = determineBgColorByLuminance(colorCandidate)
                if (resultingBgColor == desiredBgColor) {
                    add(ColorSegment(colorCandidate, degree, activeByLuminance = true, activeByConfiguration = true))
                    break@adjust
                }
                brightness += deltaBrightness
                if (brightness !in 0.0..1.0) {
                    add(ColorSegment(colorCandidate, degree, activeByLuminance = false, activeByConfiguration = true))
                    break@adjust
                }
            }
        }
    }
}

class ColorSegment(val c: Color, degree: Int, val activeByLuminance: Boolean, var activeByConfiguration: Boolean) {
    val active
        get() = activeByLuminance && activeByConfiguration
}

