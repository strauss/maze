package de.dreamcube.mazegame.ui

import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.ChatInfoListener
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import javax.swing.JTextPane
import javax.swing.text.BadLocationException
import javax.swing.text.Style
import javax.swing.text.StyleConstants

class MessagePane(private val controller: UiController) : JTextPane(), ChatInfoListener, PlayerConnectionListener {

    companion object {
        private const val OTHER_CLIENT_STYLE_PREFIX = "bot_"
        private const val OTHER_CLIENT_WHISPER_STYLE_PREFIX = "bot_whisper_"
        private val CLIENT_COLOR: Color? = Color.BLUE
        private val SERVER_COLOR: Color? = Color.RED.darker()
        private val MESSAGE_COLOR: Color? = Color.BLACK

        private val LOGGER: Logger = LoggerFactory.getLogger(MessagePane::class.java)
    }

    @Transient
    private val clientStyle: Style

    @Transient
    private val serverStyle: Style

    @Transient
    private val messageStyle: Style

    private val playerIds: MutableSet<Int> = HashSet()

    init {
        controller.messagePane = this
        val baseStyle = addStyle("base", null)
        StyleConstants.setFontFamily(baseStyle, "Arial")
        StyleConstants.setFontSize(baseStyle, 18)
        val originStyle = addStyle("origin", baseStyle)
        StyleConstants.setBold(originStyle, true)
        clientStyle = addStyle("client", originStyle)
        StyleConstants.setForeground(clientStyle, CLIENT_COLOR)
        serverStyle = addStyle("server", originStyle)
        StyleConstants.setForeground(serverStyle, SERVER_COLOR)
        messageStyle = addStyle("message", baseStyle)
        StyleConstants.setForeground(messageStyle, MESSAGE_COLOR)
        controller.prepareEventListener(this)
    }

    override fun onClientInfo(message: String) {
        controller.uiScope.launch {
            try {
                styledDocument.insertString(styledDocument.length, "Client: ", clientStyle)
                appendChatMessage(message)
            } catch (ex: BadLocationException) {
                LOGGER.error("Error while displaying client info message: ${ex.message}")
            }
        }
    }

    override fun onServerInfo(message: String) {
        controller.uiScope.launch {
            try {
                styledDocument.insertString(styledDocument.length, "Server: ", serverStyle)
                appendChatMessage(message)
            } catch (ex: BadLocationException) {
                LOGGER.error("Error while displaying server info message: ${ex.message}")
            }
        }
    }

    override fun onPlayerChat(playerId: Int, playerNick: String, message: String, whisper: Boolean) {
        controller.uiScope.launch {
            try {
                val otherClientStyle: Style
                if (whisper) {
                    otherClientStyle = addStyle(OTHER_CLIENT_WHISPER_STYLE_PREFIX + playerId, clientStyle)
                    StyleConstants.setItalic(otherClientStyle, true)
                } else {
                    otherClientStyle = addStyle(OTHER_CLIENT_STYLE_PREFIX + playerId, clientStyle)
                }
                StyleConstants.setForeground(otherClientStyle, controller.uiPlayerCollection.getById(playerId)?.color ?: Color.black)
                styledDocument.insertString(styledDocument.length, "$playerNick: ", otherClientStyle)
                appendChatMessage(message)
            } catch (ex: BadLocationException) {
                LOGGER.error("Error while displaying player chat message: ${ex.message}")
            }
        }
    }

    private fun appendChatMessage(message: String) {
        this.styledDocument.insertString(this.styledDocument.length, "$message\n", messageStyle)
        caretPosition = styledDocument.length
    }

    override fun onPlayerLogin(playerSnapshot: PlayerSnapshot) {
        playerIds.add(playerSnapshot.id)
    }

    override fun onOwnPlayerLogin(playerSnapshot: PlayerSnapshot) {
        // ignore
    }

    override fun onPlayerLogout(playerSnapshot: PlayerSnapshot) {
        val playerId = playerSnapshot.id
        removeStyleForPlayer(playerId)
        playerIds.remove(playerId)
    }

    internal fun reset() {
        for (playerId in playerIds) {
            removeStyleForPlayer(playerId)
        }
        playerIds.clear()
    }

    internal fun clear() {
        styledDocument.remove(0, styledDocument.length)
        caretPosition = 0
    }

    private fun removeStyleForPlayer(playerId: Int) {
        removeStyle(OTHER_CLIENT_STYLE_PREFIX + playerId)
        removeStyle(OTHER_CLIENT_WHISPER_STYLE_PREFIX + playerId)
    }
}