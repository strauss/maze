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

package de.dreamcube.mazegame.client.config

/**
 * Simple configuration object for the maze client.
 */
class MazeClientConfigurationDto @JvmOverloads constructor(
    /**
     * The server address.
     */
    val serverAddress: String,

    /**
     * The server port.
     */
    val serverPort: Int,

    /**
     * The name of the strategy.
     */
    val strategyName: String,

    /**
     * Should the flavor text be emitted to the server? Default is true.
     */
    val withFlavor: Boolean = true,

    /**
     * The actual nickname to be displayed. Default is [strategyName].
     */
    val displayName: String = strategyName
)