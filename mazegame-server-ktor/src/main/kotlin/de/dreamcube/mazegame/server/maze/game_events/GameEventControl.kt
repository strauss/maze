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

package de.dreamcube.mazegame.server.maze.game_events

import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.server.maze.MazeServer
import de.dreamcube.mazegame.server.maze.commands.control.PutBaitCommand
import de.dreamcube.mazegame.server.maze.commands.game.BaitRushCommand
import de.dreamcube.mazegame.server.maze.commands.game.TransformBaitsCommand
import de.dreamcube.mazegame.server.maze.createPlayerScoreChangedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class GameEventControl(private val server: MazeServer, private val parentScope: CoroutineScope) :
    CoroutineScope by CoroutineScope(parentScope.coroutineContext + SupervisorJob()) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GameEventControl::class.java)
    }

    private val gameEventChannel = Channel<GameEvent>(Channel.UNLIMITED)
    private val rng = Random.Default
    private val earliestNextEvent: AtomicLong = AtomicLong(0L)

    fun start() = launch {
        activateCooldown()
        LOGGER.info("Game event control started!")
        for (event in gameEventChannel) {
            when (event) {
                is BaitCollectedEvent -> handleBaitCollected(event)
                is PlayerCollisionEvent -> handlePlayerCollision(event)
                else -> LOGGER.warn("Unknown game event type: ${event::class.simpleName}")
            }
        }
    }

    suspend fun addEvent(event: GameEvent) {
        gameEventChannel.send(event)
    }

    private fun activateCooldown() {
        earliestNextEvent.set(System.currentTimeMillis() + server.serverConfiguration.game.events.eventCooldown)
    }

    private suspend fun handleBaitCollected(baitCollectedEvent: BaitCollectedEvent) {
        val now = System.currentTimeMillis()
        // check cooldown over
        if (now < earliestNextEvent.get()) {
            return
        }
        val eventConfiguration = server.serverConfiguration.game.events
        val roll: Double = rng.nextDouble()
        val causingPlayerId = baitCollectedEvent.player.id
        when (baitCollectedEvent.bait.type) {
            BaitType.TRAP -> { // Bait rush if trap was invisible and roll hits
                if (!baitCollectedEvent.bait.visibleToClients && roll < eventConfiguration.baitRushProbability) {
                    server.commandExecutor.addCommand(BaitRushCommand(server, causingPlayerId))
                    activateCooldown()
                }
            }

            BaitType.FOOD -> { // All gems if roll hits
                if (roll < eventConfiguration.allGemProbability) {
                    server.commandExecutor.addCommand(TransformBaitsCommand(server, BaitType.GEM, causingPlayerId))
                    activateCooldown()
                }
            }

            BaitType.COFFEE -> { // All food if roll hits
                if (roll < eventConfiguration.allFoodProbability) {
                    server.commandExecutor.addCommand(TransformBaitsCommand(server, BaitType.FOOD, causingPlayerId))
                    activateCooldown()
                }
            }

            BaitType.GEM -> { // All traps if roll hits
                if (roll < eventConfiguration.allTrapProbability) {
                    server.commandExecutor.addCommand(TransformBaitsCommand(server, BaitType.TRAP, causingPlayerId))
                    activateCooldown()
                }
            }
        }
    }

    private suspend fun handlePlayerCollision(playerCollisionEvent: PlayerCollisionEvent) {
        val now = System.currentTimeMillis()
        // check cooldown over
        if (now < earliestNextEvent.get()) {
            return
        }
        val eventConfiguration = server.serverConfiguration.game.events
        val roll = rng.nextDouble()
        val causingPlayer = playerCollisionEvent.causingPlayer
        if (roll < eventConfiguration.allCoffeeProbability) {
            // Transform all baits into coffee
            server.commandExecutor.addCommand(TransformBaitsCommand(server, BaitType.COFFEE, causingPlayer.id))
            activateCooldown()
        } else if (roll < eventConfiguration.allCoffeeProbability + eventConfiguration.loseBaitProbability) {
            // create a bait at the collision site
            val baitType = server.baitGenerator.createRandomBaitTypeNoTrap()
            // reduce points from player
            causingPlayer.score -= baitType.score
            server.sendToAllPlayers(createPlayerScoreChangedMessage(causingPlayer).thereIsMore())
            server.commandExecutor.addCommand(
                PutBaitCommand(
                    server,
                    baitType,
                    playerCollisionEvent.x,
                    playerCollisionEvent.y,
                    optionalMessage = "While ${causingPlayer.nick} ran into ${playerCollisionEvent.otherPlayer.nick}, they dropped a ${baitType.baitName}."
                )
            )
        }
    }

}