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

package de.dreamcube.mazegame.client.maze.strategy

/**
 * Used to annotate a subclass of [Strategy] and mark it as a bot strategy. If you don't attach this to your strategy,
 * neither the headless client, nor the ui will be able to find your bot.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Bot(
    val value: String,
    val isSpectator: Boolean = false,
    val isHuman: Boolean = false,
    val flavor: String = "My creator did not figure out (yet), how to change this text :-)"
)
