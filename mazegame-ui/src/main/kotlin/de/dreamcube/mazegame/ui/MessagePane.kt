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

import de.dreamcube.hornet_queen.set.PrimitiveIntSetB
import de.dreamcube.mazegame.client.maze.PlayerSnapshot
import de.dreamcube.mazegame.client.maze.events.ChatInfoListener
import de.dreamcube.mazegame.client.maze.events.PlayerConnectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import javax.swing.JTextPane
import javax.swing.text.BadLocationException
import javax.swing.text.Style
import javax.swing.text.StyleConstants

/**
 * The chat area. Reacts to chat messages and displays them.
 */
class MessagePane() : JTextPane(), ChatInfoListener, PlayerConnectionListener {

    companion object {
        private const val OTHER_CLIENT_STYLE_PREFIX = "bot_"
        private const val OTHER_CLIENT_WHISPER_STYLE_PREFIX = "bot_whisper_"
        private val CLIENT_COLOR: Color? = Color.BLUE
        private val SERVER_COLOR: Color? = Color.RED.darker()
        private val MESSAGE_COLOR: Color? = Color.BLACK

        private val LOGGER: Logger = LoggerFactory.getLogger(MessagePane::class.java)
    }

    @Transient
    private val paragraphStyle: Style

    @Transient
    private val clientStyle: Style

    @Transient
    private val serverStyle: Style

    @Transient
    private val messageStyle: Style

    @Transient
    private val appendMutex = Mutex()

    private val playerIds: MutableSet<Int> = PrimitiveIntSetB()

    private val writeMessageChannel =
        Channel<suspend () -> Unit>(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        UiController.messagePane = this
        val baseStyle = addStyle("base", null)
        StyleConstants.setFontFamily(baseStyle, "Arial")
        StyleConstants.setFontSize(baseStyle, 18)
        paragraphStyle = addStyle("paragraph", baseStyle).also {
            StyleConstants.setSpaceAbove(it, 0f)
            StyleConstants.setSpaceBelow(it, 2f)
        }

        val originStyle = addStyle("origin", baseStyle)
        StyleConstants.setBold(originStyle, true)
        clientStyle = addStyle("client", originStyle)
        StyleConstants.setForeground(clientStyle, CLIENT_COLOR)
        serverStyle = addStyle("server", originStyle)
        StyleConstants.setForeground(serverStyle, SERVER_COLOR)
        messageStyle = addStyle("message", baseStyle)
        StyleConstants.setForeground(messageStyle, MESSAGE_COLOR)
        UiController.prepareEventListener(this)
        UiController.bgScope.launch {
            for (writeMessage in writeMessageChannel) {
                delay(10L) // just delay the processing of chat messages a bit
                withContext(Dispatchers.Swing) {
                    writeMessage()
                }
            }
        }
    }

    override fun onClientInfo(message: String) {
        writeMessageChannel.trySend {
            try {
                appendMessage("Client: ", clientStyle, message)
            } catch (ex: BadLocationException) {
                LOGGER.error("Error while displaying client info message: ${ex.message}")
            }
        }
    }

    override fun onServerInfo(message: String) {
        writeMessageChannel.trySend {
            try {
                appendMessage("Server: ", serverStyle, message)
            } catch (ex: BadLocationException) {
                LOGGER.error("Error while displaying server info message: ${ex.message}")
            }
        }
    }

    override fun onPlayerChat(playerId: Int, playerNick: String, message: String, whisper: Boolean) {
        writeMessageChannel.trySend {
            try {
                val otherClientStyle: Style = if (whisper) {
                    getWhisperStyle(playerId)
                } else {
                    getClientStyle(playerId)
                }
                StyleConstants.setForeground(
                    otherClientStyle,
                    UiController.uiPlayerCollection.getById(playerId)?.color ?: Color.black
                )
                appendMessage("$playerNick: ", otherClientStyle, message)
            } catch (ex: BadLocationException) {
                LOGGER.error("Error while displaying player chat message: ${ex.message}")
            }
        }
    }

    private suspend fun appendMessage(prefix: String, prefixStyle: Style, message: String) {
        appendMutex.withLock {
            val doc = this.styledDocument
            val lineStart = doc.length
            doc.insertString(lineStart, prefix, prefixStyle)
            doc.insertString(doc.length, "$message\n", messageStyle)
            doc.setParagraphAttributes(lineStart, 1, paragraphStyle, false)
            caretPosition = doc.length
        }
    }

    private fun getWhisperStyle(playerId: Int): Style {
        val styleKey = OTHER_CLIENT_WHISPER_STYLE_PREFIX + playerId
        val style: Style? = getStyle(styleKey)
        return style ?: addStyle(styleKey, clientStyle).apply { StyleConstants.setItalic(this, true) }
    }

    private fun getClientStyle(playerId: Int): Style {
        val styleKey = OTHER_CLIENT_STYLE_PREFIX + playerId
        val style: Style? = getStyle(styleKey)
        return style ?: addStyle(styleKey, clientStyle)
    }

    override fun onPlayerLogin(playerSnapshot: PlayerSnapshot) {
        playerIds.add(playerSnapshot.id)
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
        writeMessageChannel.trySend {
            appendMutex.withLock {
                styledDocument.remove(0, styledDocument.length)
                caretPosition = 0
            }
        }
    }

    private fun removeStyleForPlayer(playerId: Int) {
        removeStyle(OTHER_CLIENT_STYLE_PREFIX + playerId)
        removeStyle(OTHER_CLIENT_WHISPER_STYLE_PREFIX + playerId)
    }
}