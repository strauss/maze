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
The decisions you make here directly influence the algorithm you pick for your search.

### Keep track of baits

The interface `BaitEventListener` handles events that are associated with the appearance and vanishing of baits.

The function `baitAppeared` gives you a [Bait](../src/main/kotlin/de/dreamcube/mazegame/client/maze/Bait.kt).
It is an immutable representation of a bait with its type, score, and coordinates.
Whenever the `baitAppeared` function is called, a new bait appeared in the maze and you might want to add it to your
internal structure and react to it.

The function `baitVanished` is the counterpart.
It is called, whenever a bait vanishes from the maze.
The most obvious reason would be, that a player has collected the bait.
It also gives a Bait object, but in this case you want to remove it from your internal structure and reevaluate.

### Keep track of players

Unlike baits, the corresponding
[Player](../../mazegame-common/src/main/kotlin/de/dreamcube/mazegame/common/maze/player.kt) object is not immutable.
The most important (literal) variables are the coordinates and the view direction.
The mentioned object is the one that is internally used.

The interface `PlayerMovementListener` handles events that are associated with the movement of players.
The functions in this interface give you a so-called
[PlayerSnapshot](../src/main/kotlin/de/dreamcube/mazegame/client/maze/PlayerSnapshot.kt).
This object is immutable and gives you a literal snapshot of the player's position whenever the event happens.

The player snapshot also contains a [PlayerView](../src/main/kotlin/de/dreamcube/mazegame/client/maze/PlayerView.kt).
This is a read-only view of the above-mentioned internal player object.
It gives you the current player information.
The access, however, is not thread-safe.
Use it carefully.

The function `onPlayerAppear` is only called once per player.
When you log into the game, it is called once for all players that are currently playing for telling you their current
position.
When another player logs into the game, it is called once for telling your, where the server initially placed them.
It just contains the snapshot.
The most important information are the id, the coordinates, and the view direction.
If you include other players into your strategy, this function is very important to keep track of all players that are
currently playing.

The function `onPlayerVanish` is called, whenever a player leaves the game.
It is even more important to pay attention to this function.
If a player leaves the game, you don't want to pay attention to their last location.

The functions `onPlayerStep` and `onPlayerTurn` are called for each move the affected player performed, so you can keep
track of them.
In addition to the snapshot, you also get their old coordinates and view direction as
[PlayerPosition](../../mazegame-common/src/main/kotlin/de/dreamcube/mazegame/common/maze/player.kt).

The last function of this interface is `onPlayerTeleport`.
It is called, whenever a player is teleported.
As in the other movement functions, you get the snapshot and the old position.
In addition to them, you learn why the player was teleported (trap or collision).
If the player was teleported due to a collision, you also get the id of the responsible player, namely the one who ran
into the other player.
This information can be useful for strategies, that try to figure out how the other players act.

### Score changes

The interface `ScoreChangeListener` allows you to react to score changes.
Usually you can rely on the statistics from the player snapshots.
However, this event provides you with the old score and the player snapshot with the new score.
This means, you can calculate the difference and ... well draw conclusions about what caused the score change.
This might sound useless, but believe me, it is not ... depending on what you want to achieve :-)

### Speed changes

The server can change the game speed at any time.
If speed changes are important for your strategy, you should implement the interface `SpeedChangedListener`.
Some advanced strategies require this information.

### Errors

The game has several error codes that can be meaningful.
During the game, the most obvious one is the wall crash.
The interface `ErrorInfoListener` provides you with the function `onServerError` and with it the corresponding error
codes.
Not all error codes are useful during game play.

The dummy bot uses the wall crash for enforcing a turn command.

### Chat

Your strategy can theoretically "listen" to the chat messages that are sent by the client, server, and other players.
The interface `ChatInfoListener` provides you with the functions `onClientInfo`, `onServerInfo`, and `onPlayerChat`.

The latter also contains the information, if another player is whispering to you.
This tiny little detail allows for inter-bot communication, if you want to create strategies involving multiple bots.
This is a possibility that has not been explored yet.

If you want to send a chat message yourself, just call the function `broadcast` or `whisper` on the `MazeClient`
reference, which is part of your strategy object (super class).

The server has two spam protection mechanism, an explicit and an implicit one.
The explicit spam protection limits the number of chat messages you can send.
Every few milliseconds you get a "chat token" (you start with some), allowing you to send one message (chat or whisper).
If your chat tokens are used up, you have to wait.
If you send a message, the server blocks it and tells you when you will get another chat token.
There is an upper limit for chat tokens that you can acquire.

The implicit spam protection makes you lose a turn for each sent message.

## React to connection related events

There are several event listener interfaces that provide you with information besides the actual game.

### Players logging in and out

The `PlayerConnectionListener` includes functions that inform you about new players joining and players leaving.
The function `onPlayerLogin` is called right before the corresponding player appears in the maze.
It is mostly used by the ui and for strategies it actually does not really matter if you react to this event or the
player appear event from above.

The function `onPlayerLogout` is called right after the corresponding player vanishes from the maze.
Here the same reasoning applies.

The function `onOwnPlayerLogin` is called, whenever your player joins the game.

### The connection status

The `ClientConnectionStatusListener` is very useful for initializing the strategy.
Depending on the connection status, certain objects are initialized and can be used.
Please refer to the documentation of this class for further details.

## Override functions/methods

In order to form your strategy, there are some functions that you can (and should) override.

### `initializeStrategy`

This function is called right after the client reference is initialized in your strategy and your strategy is registered
as event listener.
If your strategy uses other classes that want to act as event listeners, this might be a good place to initialize them.

### `beforeGoodbye`

This function is called, right before your client sends logs out.
It is less useful but can be funny.

### `getNextMove`

This is the most important function of them all.
Here you implement the core of your strategy.
A straightforward approach would be performing whatever algorithm you come up with and return the resulting move.

However, you are the creator of your strategy.
You do not have to use this approach.
The important part is, that this function should return the next move, no matter when and how it is calculated.
