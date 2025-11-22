# REST based server control

In this document the server control functions are explained in detail.
The server control allows for the following aspects:

- Provide server information (reduced and full)
- Launch and Quit servers
- Provide player information
- Control the game
- Trigger game events manually
- Contest control

## Authentication

Most of the endpoints require a bearer token to be accessed.
In order to obtain one, you need to call the endpoint

`POST /login` using basic authentication.

The username is always `master`.
The default password is `retsam`.
The password can be set on server start using the system property `masterPassword`.
Details can be found in the [README.md](../README.md).

If successful, you get a `JwtToken` as JSON response.

- `token: String` the bearer token.
- `expires: Long` after that many milliseconds the token will expire and your call will yield a 401.

Use the `token` as your bearer token for all endpoints that require it.
If you incorporate it into a client, you need to re-login before the token expires.
The `mazegame-ui` provides exactly that in the function `loginOrRefreshToken` of class `ServerCommandController`.

## Provide server information

### Reduced server information

- `GET /server`

This endpoint does not require authentication.

Gives a JSON list of `ReducedServerInformationDto`.

- `id: Int` the server ID (aka "port")
- `maxClients: Int` maximum number of client connections
- `activeClients: Int` current number of client connections (players and spectators)
- `speed: Int` current game speed in milliseconds
- `width: Int` the width of the maze
- `height: Int` the height of the maze
- `compactMaze: String` a base64 representation of a serialized `CompactMaze`
    - This can be used by the client to render a minimap before the game is selected.
- `spectatorName: String?` the name of the spectator. Clients can use this information to automatically assign the
  correct nickname if a spectator "strategy" is selected. If the spectator mode is disabled, this field is `null`.

#### Status Codes

- `200` on success

### Full server information

- `GET /server/$id/info`

This endpoint requires a bearer token.

Gives detailed information (`ServerInformationDto`) as JSON for a specific server identified by its port as ID.

- `connection: ConnectionDto` (see [configuration.md](configuration.md)) for details
- `mazeInformation: MazeInformationDto`
    - `generatorMode: GeneratorMode` how was the maze generated
    - `generatorParameters: GeneratorParametersDto` (see [configuration.md](configuration.md)) for details
    - `walkableFields: Int` the current number of walkable fields
- `gameInformation: GameInformationDto`
    - `speed: GameSpeed` the current game speed
    - `autoTrapeater: Boolean` is the auto trapeater active?
    - `allowSpectator: Boolean` is the spectator mode active?
    - `delayCompensation: Boolean` is the delay compensation active?
    - `baitInformation: BaitInformationDto`
        - `baitGenerator: BaitGeneratorDto` (see [configuration.md](configuration.md)) for details
        - `baseBaitCount: Int` the current base bait count
        - `desiredBaitCount: Int` the current desired bait count (how many baits should be in the game?)
        - `currentBaitCount: Int` how many baits are currently in the game?
        - `maxTrapCount: Int` how many traps are allowed?
        - `currentTrapCount: Int` number of traps that are currently in the game
        - `visibleTrapCount: Int` number of visible traps that are currently in the game
    - `activePlayers: Int` how many players are actually playing?
    - `availableBotNames: List<String>` a list of strings of all strategy names that can be run inside the server

#### Status codes

- `200` on success
- `400` if the `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

## Server control

### Read the current server configuration

- `GET /server/$id/info/config`

This endpoint requires a bearer token.

Gives the complete server configuration (`MazeServerConfigurationDto`) as JSON for a
specific server identified by its port as ID.

The configuration is described in [configuration.md](configuration.md)

#### Status codes

- `200` on success
- `400` if the `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

### Start a server manually

- `POST /server`

This endpoint requires a bearer token.

Takes a server configuration as JSON (`MazeServerConfigurationDto`) and tries to launch a new server with it. If
successful, the complete configuration object (including all default values that have been omitted in the request) is
returned as JSON.

The configuration is described in [configuration.md](configuration.md). The only difference is the JSON format.

#### Status codes

- `200` on success
- `401` if the bearer token is missing, invalid, or expired
- `404` on failure without exception (should never happen): "Server not found, although it should have been created."
- `500` if the creation failed with an exception

### Shutdown a server

- `POST /server/$id/control/quit`

This endpoint requires a bearer token.

Shuts down a server gracefully. The server is identified by its port as ID.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if the `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

## Player information

### Information about all players

- `GET /server/$id/info/player`

This endpoint requires a bearer token.

Gives a JSON list of `PlayerInformationDto` as response.

- `id: Int` the player/clienst id
- `nick: String` the player's nickname
- `score: Int` the player's current score
- `serverSided: Boolean` flag indicating if the player is a server-sided bot
- `delayOffset: Long` current offset in milliseconds. Positive values indicate a delay compensation. Negative values
  indicate a server-sided "penalty" (currently this is only used for slowing down a trapeater from the server side).
- `totalPlayTime: PlayTimeDto` playtime since login
    - `milliseconds: Long` playtime in milliseconds
    - `time: String` human readable representation of the playtime
- `currentPlayTime: PlayTimeDto` playtime since last score reset
- `currentPointsPerMinute: Double` server-sided information about points per minute since the last score reset
- `currentAvgMoveTimeInMs: Double` server-sided information about the average move time in milliseconds
- `spectator: Boolean` is this client a spectator?

#### Status codes

- `200` on success
- `400` if the `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

### Information about a single player

`GET /server/$id/info/player/$playerId`

This endpoint requires a bearer token.

Gives a single `PlayerInformationDto` JSON as response.
The fields are already documented above.

#### Status codes

- `200` on success
- `400` if the `$id` is not a number
- `400` if the `$playerId` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if either there is no associated server
- `404` if there is no associated player

## Game control

### Start the game (go)

- `POST /server/$id/control/go`

This endpoint requires a bearer token.

Starts the game, meaning baits will be generated.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if the `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `412` if the server is already started

### Stop the game (stop)

- `POST /server/$id/control/stop`

This endpoint requires a bearer token.

Stops the game, meaning no baits will be generated and optionally all baits are removed right away.

Query parameters:

- `now: Boolean` (optional) Should the server be stopped instantly? `false` is the default if the parameter is not set.
  This parameter also accepts `yes` and`no` and is not case-sensitive.
    - `true`: all baits are removed immediately.
    - `false`: only traps are removed immediately, remaining baits can be collected, no new baits are generated.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if the `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `412` if the server is already stopped

### Clear the scores (clear)

- `POST /server/$id/control/clear`

This endpoint requires a bearer token.

Clears the score of all players. Can optionally stop the game immediately.

Query parameters:

- `stop: Boolean` (optional) Should the server be stopped?
    - `true`: while the scores are cleared, the game is also stopped. The stop mode is "immediately", meaning all baits
      will vanish at once.
    - `false`: only the scores are cleared. All baits will remain in the game and the players can collect them.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

### Change speed

- `POST /server/$id/control/speed`

This endpoint requires a bearer token.

Changes the game speed.

Query parameters:

- `speed: String` (mandatory) What is the requested speed? The string requires to be the short name of a `GameSpeed`
  enum literal. See [configuration.md](configuration.md) for more details

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if `$id` is not a number
- `400` if the query parameter `speed` is missing
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `404` if the value of `speed` is not a correct `GameSpeed` short name

### Spawn a server-sided bot (spawn)

- `POST /server/$id/control/spawn/$botName`

This endpoint requires a bearer token.

Spawns a server-sided bot. The strategy implementation is determined by its name. If the trapeater is tried to be
spawned this way, it is not directly spawned. If the auto trapeater function was deactivated, it will be activated.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `201` on success
- `202` if trapeater was requested
- `400` if `$id` is not a number
- `400` if `$botName` is missing (should not happen)
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `404` if there is no matching strategy class for the given bot name
- `500` if the client creation failed for an unknown reason
- `503` if the server is full

### Remove a client (kill)

- `POST /server/$id/control/kill/$playerId`

This endpoint requires a bearer token.

Removes a player from the game.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if `$id` is not a number
- `400` if `$playerId` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `404` if there is no associated player

### Teleport a player (teleport)

- `POST /server/$id/control/teleport`

This endpoint requires a bearer token.

Takes a `TeleportCommandDto` as JSON and teleports the player to the given coordinates if this is possible.

- `id: Int` the id of the player/client
- `x: Int` the x coordinate of the target location
- `y: Int` the y coordinate of the target location

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if `$id` is not a number
- `400` if the coordinates are out of bounds
- `400` if the coordinates do not point to a path cell
- `400` if the target cell is already occupied by something else
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `404` if there is no associated player

### Manually place a single bait in the maze

- `POST /server/$id/control/put-bait`

This endpoint requires a bearer token.

Takes a `PutBaitCommandDto` as JSON and places a bait of the given typ to the given coordinates, if this is possible.

- `baitType: BaitType` the type of the bait. One of `food`, `coffee`, `gem`, or `trap`
- `x: Int` the x coordinate of the target location
- `y: Int` the y coordinate of the target location
- `visible: Booslean` (optional, default `true`) should the bait be visible at once?
- `reappearOffset: Long` (optional, default `0`, only meaningful if `visible` is false) after how many milliseconds
  should the bait reappear. Please note: The bait will reappear when the next player collects a bait after this time has
  passed. If you place an invisible bait into a maze without players, it will never reappear.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if `$id` is not a number
- `400` if the coordinates are out of bounds
- `400` if the coordinates do not point to a path cell
- `400` if the target cell is already occupied by something else
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

## Trigger Game events manually

### Bait rush

- `POST /server/$id/control/bait-rush`

This endpoint requires a bearer token.

Starts a "bait rush". The number of baits is temporarily doubled.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

### Transform all baits to food/coffee/gems/traps

- `POST /server/$id/control/all-food`
- `POST /server/$id/control/all-coffee`
- `POST /server/$id/control/all-gems`
- `POST /server/$id/control/all-traps`

These endpoints require a bearer token.

Transform all baits into a single bait type.
If a trapeater is present, it despawns, unless all baits are transformed into traps.
In that case, a trapeater spawns if it is not already active.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `204` on success
- `400` if `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server

## Contest control

It is possible to start a contest.
A contest runs for a certain amount of time.
When it starts, the scores are reset.
When it ends, the game is stopped and the winner is announced.
A contest can be configured with certain events that can occur during its run.
For this, several endpoints exist to control them.

Every contest can have the following events (`ContestEventType`):

- `START` starts the event
- `REPORT` reports the current status
- `SPAWN_FRENZY` spawns the frenzy bot
- `DESPAWN_FRENZY` despawns the frenzy bot
- `STOP` ends the event

There are more to come, but that's the current state.

Events can be configured with a `ContestConfiguration`.

- `durationInMinutes: Int` how long will it run? Default is 30 minutes. Negative values are allowed, but meaningless.
  The contest will stop right after it was started.
- `statusReportIntervalInMinutes: Int` when will status reports be sent out? Default is every 5 minutes.
- `statusPositions: Int` how many positions will be reported? Default is 10 (for the classical top 10). If this value is
  0 or less or exceed
- `additionalEvents: List<ContestEvent>` allows for configuring additional events. The list is not required to be sorted
  in any way. The default configuration spawns the frenzy bot after 1/3 of the time and despawns it after 2/3 of the
  time. If you don't have a frenzy bot, nothing will happen. If you don't want a frenzy bot, set this to be an empty
  list.
    - `type: ContestEventType` the type of event (see above)
    - `delayInMinutes: Int` when shall the event occur? This number should not exceed the total event time. It is not
      forbidden, but useless. If this value is zero or negative, those events will happen right after the event was
      started.

The class `ContestController` handles one contest.
Every contest will have the following events:

- A start event of `ContestEventType.START` with a delay of 0.
- A stop event of `ContestEventType.STOP` with a delay of `durationInMinutes`.

If it is configured correctly:

- Several events of `ContestEventType.REPORT` with a delay depending on the configuration.

In addition, the events from `additionalEvents` are also scheduled.

When the contest is started, at first all events are initialized.
Then, all of them are started as coroutine whose first function is a `delay` of the configured time.
After the delay, the event is actually processed and executed.

If a spontaneous report is requested, an additional report event is scheduled with delay `0`.

If a contest should be stopped unplanned, an additional stop event is scheduled with delay `0`.
This effectively cancels all pending coroutines and ends the contest.

### Retrieve contest information

- `GET /server/$id/contest`

This endpoint requires a bearer token.

If a contest is currently running, this endpoint gives its information as `ContestConfiguration` JSON.

#### Status codes

- `200` on success
- `400` if `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `404` if there is no contest running

### Start a contest

- `POST /server/$id/contest`

This endpoint requires a bearer token.

This endpoint takes a `ContestConfiguration` as JSON and starts a contest with it, if this is possible.
If no configuration is given, a default configuration is taken instead.

#### Status codes

- `202` on success
- `400` if `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `412` if there is already a contest running
- `500` if the contest could not be started for any other reason

### Stop a contest

- `DELETE /server/$id/contest`

This endpoint requires a bearer token.

This endpoint immediately stops a running contest. The result is communicated through the chat as if the contest was
stopped as planned.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `202` on success
- `400` if `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `412` if there is no contest running

### Trigger a report

- `POST /server/$id/contest/report`

This endpoint requires a bearer token.

This endpoint triggers a report to be posted to all players.

This endpoint has no request body.

This endpoint has no response body.

#### Status codes

- `202` on success
- `400` if `$id` is not a number
- `401` if the bearer token is missing, invalid, or expired
- `404` if there is no associated server
- `412` if there is no contest running
