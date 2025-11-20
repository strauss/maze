# Instructions for migrating "legacy bots"

If you want to use your "old" bot with the new fancy client, you have to migrate it to the new client structure.
You can either just migrate it to the new structure using Java.
You can, however, go the extra mile and migrate it to Kotlin.

## Structural differences

In this section, the differences between the old client and the new client are shown.

### Events

One of the main difference is a finer-grained event system.
The old client only shipped with two types of events.
The `FieldChangeEvent` covered bait and player related events.
The `ScoreChangeEvent` only covered score changes.

The new client provides several event listener interfaces in the package `de.dreamcube.mazegame.client.maze.events`.
The detailed information about those interfaces can be found in [bot.md](bot.md) or in the KDoc of the respective
interfaces.
Some of them cover the original functionality.
Others introduce new events.

### There is no maze

If you relied on the class `mazegame.client.Maze`, good luck.
You now have to decide on your own maze structure.
Old bots usually either use this structure directly or use it to create their own maze representation.

In the new client, each bot is required to implement its own representation and directly parse the maze data received
from the server.
This is a good way to do it:

- Your strategy class should implement the `MazeEventListener` interface.
- The constructor of your maze structure should have at least the `mazeLines` in its parameter list.
    - `width` and `height` might seem optional, but can also be useful. Just copy the parameter list from the
      `onMazeReceived` function and you are fine.
    - The constructor should then do the parsing.

### Baits

The class `mazegame.client.Bait` used to be the bait representation.
The type was simply a `String` and the value was an arbitrary `int`.

In the new client, the bait is represented by the class `de.dreamcube.mazegame.client.maze.Bait`.
The main difference is the enum class `de.dreamcube.mazegame.common.maze.BaitType`.
This enum represents the type as enum literal.
It also contains the score value.
Alternatively you can also use the virtual `score` field, which actually pulls the value out of the type.

If you want to react to bait events, your strategy class has to implement the `BaitEventListener` interface.
If you want to do it "the old way", you can do so with the functions `getBaits` and `getBaitsBlocking` from the class
`MazeClient`.
Retrieving this collection is somewhat thread-safe (safer than in the old client).
It is highly recommended to fully switch to the event listener interfaces.
However, for a first migration it might be feasible to use the old way.

### Players

The class `mazegame.client.Player` used to be the player representation.
It represented the current state of each player.
However, that representation was never thread-safe.

The new class `de.dreamcube.mazegame.common.maze.Player` takes over this role.
However, it is only used internally.
It is possible to get a read-only view on a player object.
This is realized with the class `de.dreamcube.mazegame.client.maze.PlayerView`.
This class is still not thread-safe.

All player-related events use the snapshot function of the player view for creating a `PlayerSnapshot`.
This is a read-only view on the player at the moment the snapshot is created.
It does not need to be thread-safe, because it is immutable.
It also contains a reference to the player view it was taken from.

The view direction is now reflected by the enum class `de.dreamcube.mazegame.common.maze.ViewDirection`.
It also contains the so-called `shortName` that used to be implemented with a char array and int constants.

If you want to react to player events, your strategy class has to implement the `PlayerMovementListener` interface.
The contained functions roughly cover all player-related former `FieldChangeEvent`s.
As with baits, you can also get the player snapshots "the old way".
For this, use the functions `getPlayers` and `getPlayersBlocking`.
This is also more thread-safe than in the old client.
You are encouraged to switch to events, but, as for baits, you can use these functions temporarily (or if you are lazy).

The player snapshots now also contain more statistics on each player.
Those are far from being perfect, but better than whatever the old client did.

### Control panel and visualization

It is still possible to use a control panel and a visualization.
Detailed information for both can be found in [bot_ui.me](bot_ui.md).
Here you find only a brief summary with focus on the differences.

The control panel will now be placed on the right side instead of the bottom.
The visualization button is now automatically available if you provide a visualization.
Therefore, it is not required anymore to implement your own inside the control panel.
The control panel can be used to control the bot and the visualization.

The visualization is intended to be "in-place".
In the new client it is separated from the glass pane that is responsible for marking the selected player.
In fact, the glass pane is now a layered pane consisting of the marker pane and the optional visualization.
The visualization is directly fed by the ui with important information, such as the zoom and the offset point.

Both control panel and visualization are available, as soon as you provide them in your bot implementation.
To activate both, click on the helm button on the right side of the status bar.
It should be available when at least one of the following is available: server control, visualization button, control
panel.

The UI is separated from the actual client.
The control panel and the visualization are the only exceptions to this.
Although theoretically possible, it is highly discouraged to "hack" your way into the ui using the component tree.
It is not intended and also not required.

If you are interacting with your visualization, don't overload the EDT, or the UI will become unresponsive.
Game events are usually not executed on the EDT.
Use `SwingUtilities.invokeLater` (or `withContext(Dispatchers.Swing)`) if you explicitly want to execute something on
the EDT.
