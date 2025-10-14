package de.dreamcube.mazegame.ui

import com.formdev.flatlaf.FlatDarkLaf
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import java.awt.EventQueue
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane

fun main() {
    // L&F setzen (FlatLaf) – modern, HiDPI-freundlich
    FlatDarkLaf.setup()

    // UI auf dem EDT starten (klassisch)
    EventQueue.invokeLater { createAndShowUI() }
}

private fun createAndShowUI() {
    // Hauptfenster
    val frame = JFrame("Minimal Swing + Coroutines").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE // Fenster schließen = App beenden
        setSize(480, 240)
        setLocationRelativeTo(null)
        contentPane.layout = MigLayout("insets 16", "[grow]", "[] push")
    }

    // Coroutine-Scope fürs UI
    val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    // Button
    val btn = JButton("Klick mich (modaler Hinweis)").apply {
        addActionListener {
            // Beispiel: erst „Hintergrundarbeit“, dann modaler Dialog auf dem EDT
            uiScope.launch {
                // Hintergrund simulieren (hier nur kurze Pause)
                delay(1000)

                // Zurück auf den Swing-Dispatcher: modalen Dialog öffnen
                withContext(Dispatchers.Swing) {
                    JOptionPane.showMessageDialog(
                        frame,
                        "Hallo! Das ist ein modales Hinweisfenster.",
                        "Hinweis",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }
    }

    // Komponenten hinzufügen (MigLayout)
    frame.contentPane.add(btn, "wrap") // eine Zeile, dann Umbruch

    frame.isVisible = true
}