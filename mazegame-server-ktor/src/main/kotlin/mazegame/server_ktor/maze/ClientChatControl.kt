package mazegame.server_ktor.maze

import de.dreamcube.mazegame.common.maze.Message
import mazegame.server_ktor.maze.ClientChatControl.Companion.NEW_TOKEN_PERIOD
import java.util.concurrent.atomic.AtomicInteger


class ClientChatControl {
    /**
     * Time of last token update.
     */
    private var lastTokenUpdateTime = System.currentTimeMillis()

    /**
     * Current tokens.
     */
    private var currentTokens = AtomicInteger(INITIAL_CHAT_TOKENS)

    /**
     * This counter is increased, whenever a message is sent. It is decreased, when a player wants to move. The number has to be zero in order to
     * allow actual movement. This is further spam protection.
     */
    private var sentMessages = AtomicInteger(0)

    /**
     * Indicates if this player has at least sent one chat message.
     */
    var everChatted = false
        private set

    /**
     * Every time the player makes a move, a token is granted if the [lastTokenUpdateTime] is more than [NEW_TOKEN_PERIOD] ms in the past.
     */
    fun onMove(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTokenUpdateTime > NEW_TOKEN_PERIOD && currentTokens.get() < MAX_CHAT_TOKENS) {
            currentTokens.incrementAndGet()
            lastTokenUpdateTime = now
        }
        if (sentMessages.get() > 0) {
            sentMessages.decrementAndGet()
            return false
        }
        everChatted = true
        return true
    }

    /**
     * Checks if sending a message is allowed. If yes, a token is consumed and true is returned. If not, false is returned. In both cases the timestamp
     * is reset. This penalizes clients that try to spam, by blocking all messages.
     */
    fun onSendMessage(): Boolean {
        val allowed = currentTokens.get() > 0
        if (allowed) {
            currentTokens.decrementAndGet()
            sentMessages.incrementAndGet()
        }
        lastTokenUpdateTime = System.currentTimeMillis()
        return allowed
    }

    companion object {
        private const val INITIAL_CHAT_TOKENS = 5
        private const val MAX_CHAT_TOKENS = 20
        private const val NEW_TOKEN_PERIOD = 2500L

        val FAILURE_MESSAGE: Message =
            createServerInfoMessage("Your chat tokens have all been consumed. You have to wait for $NEW_TOKEN_PERIOD milliseconds to get a new one.")
        val FIRST_CHAT_HINT: Message =
            createServerInfoMessage("Be aware: every chat and whisper message costs you a move. Use that power wisely!")
    }
}