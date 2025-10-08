package de.dreamcube.mazegame.common.maze

enum class InfoCode(val code: Int) {
    OK(0),
    SERVER_MESSAGE(200),
    CLIENT_MESSAGE(201),
    CLIENT_WHISPER(202),
    WRONG_PARAMETER_VALUE(450),
    TOO_MANY_CLIENTS(451),
    DUPLICATE_NICK(452),
    WALL_CRASH(453),
    ACTION_WITHOUT_READY(454),
    ALREADY_LOGGED_IN(455),
    COMMAND_BEFORE_LOGIN(456),
    LOGIN_TIMEOUT(457),
    UNKNOWN_COMMAND(500),
    PARAMETER_COUNT_INCORRECT(501),
    COMPLETELY_UNKNOWN(999);

    companion object {
        fun fromCode(code: Int) = when (code) {
            OK.code -> OK
            SERVER_MESSAGE.code -> SERVER_MESSAGE
            CLIENT_MESSAGE.code -> CLIENT_MESSAGE
            CLIENT_WHISPER.code -> CLIENT_WHISPER
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