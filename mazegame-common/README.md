# Mazegame Common

This module contains classes that can be used by all other modules.

The package `de.dreamcube.mazegame.common.api` contains everything that it (also) used by the REST endpoints.
Those are required by the server and the ui, especially for the server control.

The package `maze` contains several classes, that are important for both server and client with respect to the game
itself.
Here you can also find classes that are relevant for the game commands.
The most prominent ones are the `Command` interface and the `CommandExecutor`.
The latter is the central mechanism, that drives both the server and the client.

The package `util` contains utility classes and interfaces that can be useful in any module of this project.
Except for the `Disposer` class, all of these classes can also be useful for bot strategies.
You are highly encouraged to use them.

## Some classes that are worth mentioning

- The file [player.kt](src/main/kotlin/de/dreamcube/mazegame/common/maze/player.kt) contains all player related classes.
  Client and server share the implementation.
- The file [text.kt](src/main/kotlin/de/dreamcube/mazegame/common/maze/text.kt) contains all rules and limitations for
  bot names and nicknames (they are actually the same). Also the limitations for chat messages are handled here.
- The file [constants.kt](src/main/kotlin/de/dreamcube/mazegame/common/maze/constants.kt) contains ... you might have
  guessed it, important constants for the game.
- The class [CompactMaze](src/main/kotlin/de/dreamcube/mazegame/common/maze/CompactMaze.kt) contains a very slim and
  simple maze representation. "Good bots" won't use this structure. Its main purpose is the internal representation of
  the preview map. It is also used by the "cheap" strategies that ship with the client.
- The [AverageCalculator](src/main/kotlin/de/dreamcube/mazegame/common/util/AverageCalculator.kt) defines a way to
  calculate average values on multiple values that change over time.
    - The [AbsoluteAverageCalculator](src/main/kotlin/de/dreamcube/mazegame/common/util/AbsoluteAverageCalculator.kt) is
      defined for `Long`. It sums up everything and counts how many elements have been added. It might be useful for
      some bot strategies, but isn't used in the current client or server.
    - The
      [SimpleMovingAverageCalculator](src/main/kotlin/de/dreamcube/mazegame/common/util/AbsoluteAverageCalculator.kt)
      is the main protagonist for the delay compensation in the server. It stores a configurable amount of values in an
      array. When the internal structure is full and a new element should be added, the oldest element is removed.
- The object [VisualizationHelper](src/main/kotlin/de/dreamcube/mazegame/common/util/VisualizationHelper.kt) contains
  several functions that can support bot visualization. The functions are defined in a Java friendly way and can be
  called as static methods. 