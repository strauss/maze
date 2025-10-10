package de.dreamcube.mazegame.client;

import java.util.concurrent.CompletableFuture;

import de.dreamcube.mazegame.client.config.MazeClientConfigurationDto;
import de.dreamcube.mazegame.client.maze.MazeClient;

public class HeadlessJavaLauncher
{
   public static void main(String[] args) throws Exception
   {
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
