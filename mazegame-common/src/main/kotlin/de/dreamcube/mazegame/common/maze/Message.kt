package de.dreamcube.mazegame.common.maze

/**
 * This class represents a message to a client or server. If [lastMessage] is set, it indicates the message sender that
 * it can flush the collected messages. When sending single messages, this should always be true (therefore the default
 * value). Empty messages are ignored. If an empty message is declared as [lastMessage], the message sender simply
 * performs a flush. This is useful, if a message creator does not know if a created message is the last.
 */
class Message(val msg: String = "") {
    /**
     * Flag indicating if it is the last message of a bulk.
     */
    var lastMessage: Boolean = true
        private set

    /**
     * Can be used to literally indicate that "there is more". Effectively sets [lastMessage] to false.
     */
    fun thereIsMore(): Message {
        lastMessage = false
        return this
    }
}