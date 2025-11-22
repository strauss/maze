# Server configuration

The server expects a configuration file at `./config/maze_config.yaml` relative to the working directory.
This documents describes the configuration of a maze server.
At top level, the configuration file is a list of server configurations, meaning you can define multiple servers in one
file.

The yaml format is very strict and unforgiving.
So be careful when editing the configuration file :-)

## Minimal configuration

Almost all configuration elements have default values and are not required to be configured.
The only exception is the game port.
The following minimal configuration defines a server at port `12345` with all values set to their respective default
values.

```yaml
- connection:
    port: 12345
```

## The configuration classes in detail

All configuration related classes can be found in the file
[server_configuration.kt](../../mazegame-common/src/main/kotlin/de/dreamcube/mazegame/common/api/server_configuration.kt).

The top-level class is `MazeServerConfigurationDto`.
It contains the following attributes that are explored one at a time.

- `connection` of class `ConnectionDto` for all connection related properties.
- `maze` of class `MazeGeneratorConfigurationDto` for all maze generator related properties.
- `serverBots` of class `ServerBotsDto` for all server-side bot related properties.
- `game` of class `GameDto` for all game-related properties.

### Connection configuration (`ConnectionDto`)

- `port`: The only property that is absolutely required to be configured. The default is 0, but that value would not
  work as a port. Each server requires a different port here. If the port is blocked by the OS, the server cannot start.
  If multiple servers are configured, only those with free ports will actually start. If no server starts, the whole
  application will stop immediately.
- `maxClients`: The maximum number of clients. The default value is `20`. A client is either a player or a spectator.
  They both count.
- `loginTimeout`: After how many milliseconds should the server stop waiting for a client response and cancel the
  connection? The default value is 30K milliseconds, resulting in a timeout of 30 seconds.
- `instantFlush`: Should all messages be flushed instantly? The default value is `false`. The server messages are
  designed for minimal network package count. Whenever a command or process results in multiple messages, that have to
  be sent to a single client, they are "parked" until the last message or a special "flush" message is sent. Setting
  this to `true` simulates the behavior of the "old" server.

### Maze generator configuration (`MazeGeneratorConfigurationDto`)

- `generatorMode`: default is `random`
    - `random`: a rectangular random maze is generated
    - `template`: a map file is read from a file or resource and is randomly filled with walls
    - `map`: a map file is read from a file or resource and is taken "as is".
- `generatorParameters: GeneratorParametersDto`
    - `width` of the maze. Only relevant for `random` mode. Default is `40`, minimum value is `15`, maximum is `120`. If
      you configure out of bounds, the boundaries are enforced.
    - `height` of the maze. Only relevant for `height` mode. Default is `30`, minimum value is `15`, maximum is `90`. If
      you configure out of bounds, the boundaries are enforced.
    - `mapFile` path to a file or resource. Default is `null`. If the file is not configured when in mode `template` or
      `map`, or the file is not found, the generator falls back to `random` mode. Therefore, it can be advisable to
      always configure the `with` and `height` or rely on the default values.
    - `mapFileMode`
        - `RESSOURCE`: The map file is read as ressource and therefore has to be in the classpath. The relative scan
          starts in package `de.dreamcube.mazegame.server.maze.generator`. There are several predefined map files
          available:
            - `contest.mzt`: A map intended to be used in `template` mode. It is relatively big and should be a good
              choice for contests.
            - `donut.mzt`: An unusual map, shaped like a donut. Intended to be used in `template` mode.
            - `empty.mzm`: A small maze, intended to be used as test map in `map` mode.
            - `square.mzt`: An interesting looking square shaped map intended to be used in `template` mode. Comes with
              predefined starting points for the random wall generator.
            - `test_chamber.mzm`: A map designed for analysing bots. Intended to be used in `map` mode. Since it is a
              single corridor, `template` mode would not work anyway.
        - `FILE`: The map file is read as file and therefore is not required to be in the classpath. The relative scan
          starts in the working directory.
    - `templateFillStartPoints`: The default is `false`. This is a very fine-grained option for the `template` mode. It
      is possible to define the starting points for the wall generator with a `?` in the map file. If you use none, this
      flag is meaningless (or assumed `true`). If at least one `?` is present in the template file, only those positions
      are used as starting point for the wall generator. If the flag is set to `true`, you are guaranteed to get enough
      starting points in your maze. Using too few starting points can lead to very unpractical mazes. The maze walls "
      grow" like plants. Those starting points are like "seeds". If you plant too few, you will end up with bigger "
      plants" that result in less junctions and require more detours for the players. The extreme case of only one `?`
      can lead to very long paths between two cells that are actually close too each other, effectively ruining every
      strategy whose target selection relies on manhattan distance (such as the built-in trapeater.

### Server-side bots configuration (`ServerBotsDto`)

- `autoLaunch`: A list of strategy names that should be launched instantly when the server starts. Strategies are
  determined using the `@Bot` annotation on a class implementing the `Strategy` interface. The annotation requires a bot
  name to be given and this bot name can be referred in thes list. The default value is an empty list. If you want
  multiple instances of the same but (such as a "dummy"), just list them multiple times.
- `specialBots: SpecialBotsDto`: A configuration for special bots. Those bots won't receive any delay compensation. Some
  of them can be
  accelerated and decelerated by the server.
    - `dummy`: The name of the server-side dummy strategy. The default value is `dummy`. Server-sided dummy bots won't
      get delay compensation.
    - `trapeater`: This bot spawns, whenever too many traps are present. Its speed is adjusted by the server. The more
      traps are on the map, the faster the bot will move. The maximum number of traps is limited (except for a certain
      game event).
    - `frenzy`: This special bot is a "just for fun" bot. If it implements the interface `SurpriseBot`, its speed is
      doubled, if the function `surpriseModeActive` returns `true`. If it doesn't implement this interface, it will be
      just a bot without delay compensation. Only one frenzy bot strategy is allowed per server. It has to be either
      spawned right away (see `autoLaunch`), manually, or scripted in a contest.
    - `spectator`: This is not really a reference to a strategy. Any client that connects with this nickname will be
      considered a spectator and never enter the maze and never receive a "RDY." command. This information is also part
      of the meta information to the client. Clients that can query for this information can automatically set the
      nickname correctly if the user just wants to spectate the game.
- `nickMappings`: Allows for custom nicknames for all server-sided bots. Defaults to "all bots should only have their
  strategy name as nickname". The strategy name is always included. This option is also more of a "fun option".
    - `dummyNames`: Alternative names for dummy bots.
    - `trapeaterNames`: Alternative names for the trapeater.
    - `frenzyNames`: Alternative names for the frenzy bot.
    - `freeNickMappings: List<FreeNickMapping>`: Mappings for all other bots, that are no "special" server bots. The
      default value is an empty list.
        - `botName`: The name of the strategy. Has to be set for every entry.
        - `additionalNames`: besides the strategy names, this set contains all alternative names that might be used for
          this bot strategy. The default value is an empty set.

### Game configuration (`GameDto`)

- `initialSpeed`: The game speed in which the server will start. Default is `normal` (150ms). The game speed can be
  changed any time using the server controls.
    - `unlimited` (1ms): Don't use it ... you have been warned.
    - `ultra` (50ms): very fast. Good for testing.
    - `fast` (100ms)
    - `normal` (150ms)
    - `slow` (200ms)
    - `ultra-slow` (300ms)
- `generateBaitsAtStart`: Default is `true`. If set to `false`, the game will start in stopped mode.
- `autoTrapeater`: Boolean: Default is `true`. Only set this to `false` if you are testing and don't want disruption.
  The traps are limited, but you don't want to be overwhelmed by them. If active, whenever the traps exceed a certain
  threshold, a trapeater can spawn randomly. If a lower threshold is undercut, there is a chance for it to despawn
  again.
- `allowSpectator`: Default is `true`. Spectator mode is only allowed, when this is active. Only set it to `false` if
  you require full control over all connection slots.
- `delayCompensation`: Default is `true`. This activates the delay compensation feature. This is a carefully crafted
  approach for compensating the ping (and unfortunately also the computation time) of the bots. It has some "cheating
  potential", but if it is overdone, the mechanism resets and penalizes the cheater slightly. The current offset for
  each player can be queried using the server controls.
- `baitGenerator: BaitGeneratorDto`: Configuration for the bait generation.
    - `objectDivisor`: The number of baits is determined by the number of walkable fields divided by this number. The
      default value is 26. The value is coerced in the range `1..walkableFields`, resulting in having at least one bait.
    - `trapDivisor`: The maximum number of traps is determined by the number of baits divided by this number. The
      default value is 4. The value is coerced in the range `1..baseBaitCount`, resulting in allowing for at least one
      trap.
- `events: SpecialEventsDto`: configures the probabilities of special events. All probabilities should be given between
  0.0 and 1.0.
    - `enabled`: Ar events enabled in general. Default is `false`.
    - `eventCooldown`: Number of milliseconds in which no events will occur after an event happened. The default value
      is 90K, resulting in at lesat 90 seconds without events after each event.
    - `allTrapsProbability`: Probability of transforming all baits into traps, whenever a gem is collected. The default
      value is 0.01 (1%). If this happens, a trapeater is forced to spawn. If no trapeater exists, this event will not
      occur at all.
    - `allFoodProbability`: Probabililty of transforming all baits into food, whenever a coffee is collected. The
      default value is 0.01 (1%).
    - `allCoffeeProbability`: Probability of transforming all baits into coffee, whenever two players collide. The
      default value is 0.01 (1%).
    - `allGemProbability`: Probability of transforming all baits into gems, whenever a food is collected. The default
      value is 0.005 (0.5%).
    - `baitRushProbability`: Probability for a "bait rush". Whenever this event occurs, the number of baits is doubled.
      However, no new baits will be generated, until the usual level is reached. THe default value is 0.05 (5%).
    - `loseBaitProbability`: Probability of losing a bait on player collision. The bait will be lost by the player who
      ran into the other player (this is always unambiguous). The affected player will also lose the points associated
      with the bait. The default value is 0.2 (20%).