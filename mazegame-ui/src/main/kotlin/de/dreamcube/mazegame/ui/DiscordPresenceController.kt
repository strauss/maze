/*
 * Maze Game
 * Copyright (c) 2026 Sascha Strauß
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

import io.github.vyfor.kpresence.ConnectionState
import io.github.vyfor.kpresence.RichClient
import io.github.vyfor.kpresence.exception.PipeNotFoundException
import io.github.vyfor.kpresence.rpc.ActivityType
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object DiscordPresenceController {
    private const val CLIENT_ID: Long = 1468366279801966692L
    private const val REPO_URL: String = "https://github.com/strauss/maze"
    internal const val DISCORD_INTEGRATION_ENABLED_BY_DEFAULT = true

    private val LOGGER: Logger = LoggerFactory.getLogger(DiscordPresenceController::class.java)

    private var client: RichClient? = null
    internal var enabled = AtomicBoolean(DISCORD_INTEGRATION_ENABLED_BY_DEFAULT)

    fun enable(botName: String) {
        if (!enabled.get()) return
        val c = RichClient(CLIENT_ID).also { client = it }
        UiController.bgScope.launch {
            if (c.connectionState == ConnectionState.DISCONNECTED) {
                try {
                    c.connect(shouldBlock = true)
                } catch (ex: PipeNotFoundException) {
                    LOGGER.warn("Could not find a running instance of the Discord desktop app.")
                    return@launch
                }
            }

            LOGGER.info("Connection to Discord App successfully established. Connection status is ${c.connectionState}")

            c.update {
                type = ActivityType.COMPETING
                details = "Playing as '$botName'"

                state = "Wanna join? Check out the..."

                button("Project on GitHub", REPO_URL)
            }

            LOGGER.info("Successfully updated status in Discord App.")
        }
    }

    fun disable() {
        client?.let { c ->
            c.clear()
            c.shutdown()
            LOGGER.info("Connection to Discord App closed. Connection status is ${c.connectionState}")
            client = null
        }
    }
}
