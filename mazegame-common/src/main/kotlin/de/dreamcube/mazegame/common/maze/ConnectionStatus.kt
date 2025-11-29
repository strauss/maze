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
 * Enum class for the connection state.
 */
enum class ConnectionStatus() {
    NOT_CONNECTED, CONNECTED, LOGGED_IN, SPECTATING, PLAYING, DYING, DEAD;

    override fun toString(): String = when (this) {
        NOT_CONNECTED, DEAD -> "Disconnected"
        CONNECTED -> "Logging in..."
        LOGGED_IN -> "Logged in"
        SPECTATING -> "Spectating"
        PLAYING -> "Playing"
        DYING -> "Logging out..."
    }
}