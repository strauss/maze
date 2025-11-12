package de.dreamcube.mazegame.common.maze

/**
 * Info codes for server errors, chat messages and speed changes.
 */
enum class InfoCode(val code: Int) {
    /**
     * Internal code, mainly used by the server.
     */
    OK(0),

    /**
     * A chat message created by the server itself.
     */
    SERVER_MESSAGE(200),

    /**
     * A chat message from one client to all others.
     */
    CLIENT_MESSAGE(201),

    /**
     * A chat message from one client to one other (whisper).
     */
    CLIENT_WHISPER(202),

    /**
     * A speed change.
     */
    SPEED_CHANGE(300),

    /**
     * The parameter value was incorrect. Mainly used for indicating invalid nicknames.
     */
    WRONG_PARAMETER_VALUE(450),

    /**
     * The server is "full".
     */
    TOO_MANY_CLIENTS(451),

    /**
     * The nickname is already taken.
     */
    DUPLICATE_NICK(452),

    /**
     * The bot tried to step into a wall. Can be used for dummy bots.
     */
    WALL_CRASH(453),

    /**
     * The client sent a move command without waiting for "RDY."
     */
    ACTION_WITHOUT_READY(454),

    /**
     * The client tried to log in after being already logged in.
     */
    ALREADY_LOGGED_IN(455),

    /**
     * The client tried to send a command without being logged in.
     */
    COMMAND_BEFORE_LOGIN(456),

    /**
     * The login timed out (from the server's perspective).
     */
    LOGIN_TIMEOUT(457),

    /**
     * The client sent an unparseable command.
     */
    UNKNOWN_COMMAND(500),

    /**
     * The expected number of parameters was incorrect.
     */
    PARAMETER_COUNT_INCORRECT(501),

    /**
     * Internal code for an unknown error code.
     */
    COMPLETELY_UNKNOWN(999);

    companion object {
        fun fromCode(code: Int) = when (code) {
            OK.code -> OK
            SERVER_MESSAGE.code -> SERVER_MESSAGE
            CLIENT_MESSAGE.code -> CLIENT_MESSAGE
            CLIENT_WHISPER.code -> CLIENT_WHISPER
            SPEED_CHANGE.code -> SPEED_CHANGE
            WRONG_PARAMETER_VALUE.code -> WRONG_PARAMETER_VALUE
            TOO_MANY_CLIENTS.code -> TOO_MANY_CLIENTS
            DUPLICATE_NICK.code -> DUPLICATE_NICK
            WALL_CRASH.code -> WALL_CRASH
            ACTION_WITHOUT_READY.code -> ACTION_WITHOUT_READY
            ALREADY_LOGGED_IN.code -> ALREADY_LOGGED_IN
            COMMAND_BEFORE_LOGIN.code -> COMMAND_BEFORE_LOGIN
            LOGIN_TIMEOUT.code -> LOGIN_TIMEOUT
            UNKNOWN_COMMAND.code -> UNKNOWN_COMMAND
            PARAMETER_COUNT_INCORRECT.code -> PARAMETER_COUNT_INCORRECT
            else -> COMPLETELY_UNKNOWN
        }
    }
}