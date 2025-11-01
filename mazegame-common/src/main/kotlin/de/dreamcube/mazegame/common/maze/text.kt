package de.dreamcube.mazegame.common.maze

import java.text.Normalizer

private val NICK_REGEX = """^[\p{L}&&\p{sc=Latin}](?:[\p{L}&&\p{sc=Latin}]|[0-9_-])*$""".toRegex()
private val CHAT_BLACKLIST = """[^0-9_\-\p{Zs}[\p{L}&&\p{sc=Latin}][\p{Punct}&&[^;]]]""".toRegex()
private const val FLAVOR_TEXT_MAX_LENGTH = 255

/**
 * Checks, if a nickname is valid. Nicknames may include some special characters that are derived from latin characters.
 * Also underscore and minus are allowed.
 */
fun isNickValid(nick: String): Boolean {
    val normalizedNick: String = Normalizer.normalize(nick, Normalizer.Form.NFC)
    return NICK_REGEX.matches(normalizedNick)
}

/**
 * Chat messages may not contain the semicolon sign. They may not contain any fancy characters. No emojis, no exotic
 * scripts. Forbidden characters are replaced with [repl] and it defaults to the evil Unicode-Error-Character.
 */
fun String.sanitizeAsChatMessage(repl: String = "�"): String {
    val normalizedMessage = Normalizer.normalize(this, Normalizer.Form.NFC)
    return normalizedMessage.replace(CHAT_BLACKLIST, repl)
}

/**
 * Flavor text follows the same rules as chat text. It is limited to [FLAVOR_TEXT_MAX_LENGTH] characters. If this limit
 * is exceeded, the string is truncated and the last three characters are replaced with "..." to indicate "there would
 * be more".
 */
fun String.sanitizeAsFlavorText(repl: String = "�"): String {
    if (this.length <= FLAVOR_TEXT_MAX_LENGTH) {
        return this.sanitizeAsChatMessage(repl)
    }
    return this.sanitizeAsChatMessage(repl).take(FLAVOR_TEXT_MAX_LENGTH - 3) + "..."
}