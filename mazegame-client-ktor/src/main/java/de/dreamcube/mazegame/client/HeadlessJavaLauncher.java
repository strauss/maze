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

package de.dreamcube.mazegame.client;

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto;
import de.dreamcube.mazegame.client.maze.MazeClient;

import java.util.concurrent.CompletableFuture;

public class HeadlessJavaLauncher {
    public static void main(String[] args) throws Exception {
        final MazeClientConfigurationDto config = new MazeClientConfigurationDto("localhost", 12344, "aimless");
        final MazeClient mazeClient = new MazeClient(config);
        final CompletableFuture<Void> unitCompletableFuture = mazeClient.startAndWait();
        Thread.sleep(5000L);
        mazeClient.logoutBlocking();
        unitCompletableFuture.thenRun(() ->
        {
        });
    }
}
