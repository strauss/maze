# Maze Client

In this module the core of the new maze client is located.
Unlike the previous client (or the clients you might have developed yourself in the past), the UI part is strictly
separated from the actual network/game part.

This module contains the client itself, without the UI part.
However, some tiny little UI bits are not avoidable.
Strategies can have a control panel and a visualization.

## Package overview

- `de.dreamcube.mazegame.client`
    - Contains an example on how to launch a headless client
    - Contains several event listeners that can print out certain game event related messages to the console
    - Contains a simple duplicate nick handler that counts up a numeric suffix (= "the old behavior")
- `de.dreamcube.mazegame.client.maze`
    - Contains the core functionality
    - Contains the player and bait representation, including the player view and the player snapshot
- `de.dreamcube.mazegame.client.maze.commands`
    - Contains all commands from the server
- `de.dreamcube.mazegame.client.maze.events`
    - Contains the event listeners and event handler
- `de.dreacmube.mazegame.client.maze.strategy`
    - Contains the base functionality for strategies/bots (see [bot.md](doc/bot.md) on how to create a bot)
    - Contains the visualization component's abstract class (see [bot_ui.md](doc/bot_ui.md) on how to implement a
      visualization)
    - Contains the dummy bot and the trapeater
    - Contains a debug visualization for the dummy bot
