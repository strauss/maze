# Communication protocol of the Maze Game

All messages are given in CSV format separated by a semicolon (`;`).
The protocol version is still "1".
However, at several points it was expanded.
Former clients should ignore additional parameters in order to work with the new server generation.

Protocol extensions are marked as such.

## Formal definition basics

### Messages

```
Message ::= ClientMessage | ServerMessage | Info
ClientMessage ::= Bye | Hello | MazeQuery | Step | Turn
ServerMessage ::= ServerVersion | BaitPos | Maze | PlayerPos | PlayerScore | 
                  Welcome | Join | Leave | Terminate | Ready | Quit 
```

### Coordinates

```
Coord ::= Integer Integer
```

The first value is the x coordinate and the second value is the y coordinate.
Both coordinates are positive integers and start counting at 0 at the north-west corner of the map.

Example: `5;10`

### Player ID

```
PlayerID ::= Integer
```

Unique identifier for each player.
It is always a positive integer starting at 1.
IDs are not reused.

Example: `42`

### Player nickname

```
NickName ::= Letter { Letter | Digit }
```

A nickname consists of letters and digits.
It has to start with a letter.

If a nickname is not allowed, the server should respond with a "INFO;450".

The server can decide if it wants to limit the length of nicknames.
If the nickname is considered too long, it has to respond with a "INFO;450".

Example: `ninja`

#### Extensions

Servers are allowed to soften the rules for nicknames.
They can allow for any UTF-8 character or limit their usage.
They should, however, stick to letters as first character.

A good compromise would be allowing for latin characters and some special ones, such as "-" and "_".

### View direction

```
ViewDir ::= "n" | "e" | "s" | "w"
```

The letters correspond to the orientation (compass).

### Turn direction

```
TurnDir ::= "r" | "l"
```

Corresponds to right or left.

### Bait type

```
BaitType ::= "gem" | "food" | "coffee" | "trap"
```

## Client messages

### Hello (HELO)

```
Hello ::= "HELO" NickName [Flavor]
Flavor ::= StringWithoutSemicolon
```

A client wants to join the game and communicates its nickname.
The server can either accept it and respond with a `WELC` message or deny it and respond with an `INFO` message.

Example: `HELO;ninja`

#### Extensions

In addition to the nickname, clients are allowed to give a flavor text to the server.
The server will emit this text to other players.
Clients can present the flavor text of other players.
Why? Because it is fun!

The flavor text should be limited.
If it is too long, the server should truncate it.
As for nicknames, the allowed charset can be any UTF-8 character.
It is advisable to employ the same character limits, as for nicknames.

Example: `HELO;chopstick;The chopsticks in your hand think you should start eating now!`

### MazeQuery (MAZ?)

```
MazeQuery ::= "MAZ?"
```

With this message, the client queries for the maze map and the positions of all players and baits.

### Step (STEP)

```
Step ::= "STEP"
```

The client sends this message after `RDY.` if it wants to move one step into the current view direction.

### Turn

```
Turn ::= "TURN" TurnDir
```

The client sends this message after `RDY.` if it wants to turn left or right.

Example: `TURN;r`

### Bye (BYE!)

```
Bye ::= "BYE!"
```

The client uses this message for leaving the game.

## Server messages

### BaitPos (BPOS)

```
BaitPos ::= "BPOS" Coord BaitType Event
Event ::= "app" | "van"
```

This message indicates a bait change.
It is sent from the server whenever a bait vanishes (has been collected) or appears.

Examples:

- `BPOS;2;5;gem;van`
    - At position x=2, y=5 a gem has vanished.
- `BPOS;42;47;trap;app`
    - At position x=42, y=47 a trap has appeared.

### Join (JOIN)

```
Join ::= "JOIN" PlayerID NickName [Flavor]
```

The server uses this message to inform all clients, that a new player has joined the game.

Example: `JOIN;13;ninja`

- `ninja` joined the game with ID `13`.

#### Extensions

The server uses the JOIN command to emit the flavor text of each client to the other clients.

Example: `JOIN;42;chopstick;The chopsticks in your hand think you should start eating now!`

### Leave (LEAV)

```
Leave ::= "LEAV" PlayerID
```

The server uses this message to inform all clients, that a player has left the game.

Example: `LEAV;13`

- Client with ID `13` left the game.

### Maze (MAZE)

```
MAZE ::= "MAZE" Width Height
Width ::= Integer
Height ::= Integer
```

The server uses this message to indicate that the maze map will follow.

Example: `MAZE;20;15`

- In this example, 15 lines of map data will follow.

```
####################
#..................#
#.##########..#.#..#
#.........#..#####.#
#.#.###.#........#.#
#.#...#.###.##.#...#
#.##..#.#......#.#.#
#..####...#.#.##.#.#
#.......#####....#.#
#.#####.....####.#.#
#.#...##.##..#.#.#.#
#.##..#...##.#.....#
#.#..####..#.#.###.#
#..................#
####################
```

### PlayerPos (PPOS)

```
PlayerPos ::= "PPOS" PlayerId Coord ViewDir Reason [ TeleportReason [ PlayerID ] ]
Reason ::= "tel" | "app" | "van" | "mov" | "trn"
TeleportReason ::= "t" | "c"
```

The server uses this command to communicate player position changes.
The following changes are possible and reflected by the reasons:

- `tel` (teleport): The player is teleported to the given coordinates in the given view direction
- `app` (appear): The player spawned at the given coordinates in the given view direction (after JOIN)
- `van` (vanish): The player has vanished (after LEAVE)
- `mov` (move): The player has moved to the given coordinates (after STEP)
- `trn` (turn): The player has turned into the given view direction (after TURN)

#### Extensions

The player pos command was extended in a backwards compatible way.
In case of a teleportation a reason for the teleportation can be given.
These reasons are:

- `t` (trap): If the player was teleported after running into a trap.
- `c` (collision): If the player was teleported after a collision with another player.

In case of a collision, the player id of the player that caused the collision is also provided.
This information can be useful for advanced bot strategies.

Examples:

- `PPOS;13;4;5;n;trn`
    - Player with id `13` is at position x=4, y=5 and turned north
- `PPOS;13;30;32;tel;t`
    - Player with id `13` ran into a trap and was teleported to position x=30, y=32
- `PPOS;13;1;1;tel;c;1337`
    - Player with id `1337` ran into player with id `13`. Player with id `13` was teleported to position x=1, y=1 as a
      consequence.

### PlayerScore (PSCO)

```
PlayerScore ::= "PSCO" PlayerID Score
Score ::= Integer
```

This command is used for updating the score of a player as information to all clients.

Example: `PSCO;13;314`

- The score of player with id `13` has changed to `314`

### Ready (RDY.)

```
Ready ::= "RDY." [Speed]
Speed ::= Integer
```

The server uses this to indicate that the client can now send its next STEP or TURN message.

#### Extensions

The protocol has been extended to communicate the current game speed.
The server can change the speed at any time and clients might want to react on speed changes.

Example: `RDY.;100`

- The client is allowed to send the next command. The game runs at 100 ms/tick.

### Quit (QUIT)

```
Quit ::= "QUIT"
```

The server uses this to tell the client that the logout was successful.

### ServerVersion (MSRV)

```
ServerVersion ::= "MSRV" Integer
```

This is the initial message from server to client.
It tells the client about the protocol version (still `1`) and implicitly asks it to start the connection "handshake".

### Terminate (TERM)

```
Terminate ::= "TERM"
```

If the server shuts down, all clients get the TERM command.
Could be used in the future to indicate a server-sided "kill".
Currently, the connection is just terminated and all other clients get a vanish notification.

### Welcome (WELC)

```
Welcome ::= "WELC" PlayerID
```

The server uses this to confirm the connection.
It also assigns an ID to the client with this message.

Example: `WELC;13`

## Info messages (INFO)

```
Info ::= "INFO" InfoCode [ Message [ PlayerID ] ]
InfoCode ::= Integer
Message ::= StringWithoutSemicolon
```

The info command used to be a pure server command.
For backwards compatibility, this command has been extended to allow for a chat functionality on the client side.

The server can send a text message to a client.
For this purpose, only the message is appended.

A client can broadcast a text message to all other clients.
In this scenario also only the text message is appended.

A client can whisper a text message to a specific other client.
In this scenario the player-ID of the sender/receiver is also appended.

The info code is one of the following:

### Text messages

- 200: Server text message to client
- 201: Client broadcast text message to server (and also to the other clients that will receive the message)
- 202: Client whisper text message to server (and also to the other client that will receive the message)

### Temporary errors

- 450: Wrong parameter value
- 451: Server full (too many clients)
- 452: Duplicate nickname
- 453: Tried to step into a wall
- 454: Did not wait for RDY. when sending STEP or TURN
- 455: Already logged in (second HELO)
- 456: Not logged in (STEP/TURN before HELO)
- 457: Login timeout

### Permanent errors

- 500: Unknown command
- 501: Wrong number of parameters

Examples:

- `INFO;451`
- `INFO;200;ninja found an invisible gem!`
- `INFO;202;Gotcha!;1337`

#### Extensions

The whole text message mechanism is a protocol extension.
In the original version of the protocol, the INFO command was solely used from server to client for communicating
errors.
With the text messages, Clients are also allowed to send INFO commands.
The server is allowed to limit the size and frequency.
However, the server only "understands" commands with the text message info codes.

## Handshake

Here an example how a typical session looks like:

- A client connects to the maze server.
- Server: `MSRV;1`
- Client: `HELO;ninja`
- Server: `WELC;13`
- Client: `MAZ?`
- Server: `MAZE;40;30`
    - Followed by the complete map
    - Followed by several PPOS and BPOS commands, including the initial position of the new client
    - Followed by the first `RDY.`
- The client now responds each ready command with either a step or a turn command.
- In addition, the server sends a lot of PPOS and BPOS commands to reflect the movements of the new client and all other
  clients
- Client: `BYE!`
- Server: `QUIT`