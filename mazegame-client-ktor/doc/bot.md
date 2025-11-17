# How to create your own strategy/bot

In order to create a bot, the following criteria have to be met.

* Create a specialization of the
  class [Strategy](../src/main/kotlin/de/dreamcube/mazegame/client/maze/strategy/Strategy.kt)
* Annotate your class with the [@Bot](../src/main/kotlin/de/dreamcube/mazegame/client/maze/strategy/Bot.kt) annotation
    * Give your bot a name.
    * Give your bot a flavor text.
* Have a no-args constructor.
* Implement the function/method `getNextMove`

In doing all the above, you will be able to create a worse version of a dummy bot, but nothing more.
In order to create a good bot, you will need to react to the events that are offered by the game.
Every strategy automatically implements the
empty [EventListener](../src/main/kotlin/de/dreamcube/mazegame/client/maze/events/eventListeners.kt) interface.
Depending on what you want to achieve, there are several concrete interfaces you can pick from.
The instance of your strategy is automatically registered as event listener for all events corresponding to the
interfaces you chose to implement.

## Pick a data model and parse the maze

The most important interface is the `MazeEventListener`.
It provides a function for receiving the maze lines from the server.
This function also gives the dimensions of the maze.

Let your strategy implement this interface and use the function to create your own data structure for the maze map.
The length of the lines list should be identical to the parameter `height`.
The length of each line should be identical to the parameter `with`.

* Each `.` character is considered a "path" and therefore walkable.
* Each `#` character is considered a "wall" and not walkable.
* Each `-` character is considered "outside" and not walkable. Sometimes the outside fields are used inside for artistic
  reasons. Just treat is as a different kind of "wall" and you will be fine.
* Every other character is considered "unknown" and not walkable. This should not be contained in the map data, but if
  it is, just treat it as outside or wall.
    * It is possible to get question marks. This is a sign for a badly configured server.

The kind of structure you pick should be based on the algorithm you want to use for your search of the best target.
Here are some ideas/examples:

* An array or an array-like structure
    * Pretty obvious choice, but with limitations
    * Easy to handle, but sometimes a bit clunky to search in
    * Used by basic bots, such as the trapeater
* A direct or implicit graph
    * Harder to handle, but easier to search in
    * More flexibility
    * Might use an array as index structure
    * Used by more advanced bots (yes, there are also advanced bots, that use an array)
* Something more exotic/eccentric
    * Here your phantasy is the only limit

## React to game events

The two interfaces for this are `BaitEventListener` and `PlayerMovementListener`.
In order to effectively play, you have to keep track of the bait and player positions.
Those two interfaces can help you with that.
You can either include the position data directly in your map data structure or keep it separately.
The decisions you make here directly influence the algorithms you pick for your search.

### Keep track of baits

The interface `BaitEventListener` handles events that are associated with the appearance and vanishing of baits.
The functions are 