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