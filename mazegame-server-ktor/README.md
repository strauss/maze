# Mazegame Server

This module contains the mazegame server application.
It is capable of hosting multiple actual servers simultaneously.

## Quickstart

- Build the project with at least `gradlew shadowJar`
- Locate the biggest Jar file in the build output directory of this and place it wherever you like
    - The name will be something like `mazegame-server-ktor-<version>-all.jar`
- create a folder named `config` and place a file named `maze_config.yaml` in there with the following content (the port
  is your choice)

```yaml
- connection:
    port: 12345
```

- Navigate to the directory with the jar and launch the application with
  `java -jar mazegame-server-ktor-<version>-all.jar`

For more details on how to configure the server read [configuration.md](doc/configuration.md).

## Features

Most features are configurable and optional.
See [configuration.md](doc/configuration.md) for more details on how to control them.

### General

- Start multiple servers in the same application
- REST server control functions with password protection
- Message consolidation for less network packages

### Maze generator

- Includes an extended version of the original random maze generator
    - You specify the dimensions and the generator creates a random rectangular maze
- Support for template files, allowing for more eccentric map forms (like a donut) with a random maze generated into it
    - Several template files are already included (see [configuration.md](doc/configuration.md) how to access them)
- Support for map files without generation

### Game features

- Multiple speed modes allowing for faster gameplay
- Delay compensation for fairer contests, even with remote participants
    - Contains some anti-cheat routines for avoiding exploitation ... yes, it was tried :-)
- Server-Side bots (default versions ship with the server, but they can be exchanged to your own implementations)
    - Dummy: Mobile roadblocks designed to disturb more advanced bot strategies
    - Auto-Trapeater: spawns when too many traps are on the map and vanishes after a while
    - Frenzy-Bot: This is a surprise bot for contests. Its behavior depends on the implementation. Can move at double
      speed
- Possibility to launch arbitrary bots inside the server
- Configurable alternative nicknames for all server-side bots, including the arbitrary ones
- Spectator-Mode: A configurable nickname that will only get all the movement information, but not participate in the
  game
- Special game events
    - Turn all baits into one specific type
    - Bait rush (temporarily more baits)
    - Players might lose a bait when they collide
- "Adjusted" placement logic for baits and teleportations
    - Players tend to be teleported into "dead ends" (one walkable neighbor) and "hallways" (two walkable neighbors)
    - Gems tend to be generated into "dead ends" and "hallways"
    - Traps tend to spawn on "junctions" (three or four walkable neighbors)
- Invisible baits (currently only gems and traps can spawn invisible) will appear after a while

### Contests

- Built-In contest mode
- Highly configurable with timed events
- Reports the results in the end
- Controllable with the REST server control

### Protocol extensions (compared to the original protocol)

Those extensions are backwards-compatible, if clients ignore "too many parameters" when parsing the existing commands.

- INFO command for In-Game-Chat: Bots can broadcast messages and talk to each other. The server can send out
  notifications to players.
    - Spam protection is included
- INFO command for Game speed changes to be transmitted to clients
- Nicknames can now contain more different characters (but still require to start with a letter)
- HELO command: Clients can now communicate their flavor text to the server
- JOIN command: The server uses it to communicate the flavor text to the other clients ... for fun
- PPOS command: It now also contains a teleportation reason and, in case of collisions, who was responsible

## More advanced start options

All of these options can be changed simultaneously.
They just use system properties.
Just keep in mind to call the -jar option as the last and you will succeed.

### Change the HTTP port

By default, the HTTP port is set to 8080.
You can override this, using a system property when launching the application.

`java -D"ktor.deployment.port"=4242 -jar mazegame-server-ktor-<version>-all.jar`

### Change the configuration file path and name

By default, the configuration file is expected to be located in the directory `config` in the working directory.
The name is expected to be `maze_config.yaml`.

You can override this using a system property when launching the application.
`java -D"mazegame.config-path"="fancy_folder/fancy_config.yaml" -jar mazegame-server-ktor-<version>-all.jar`

### Change the master password for the server control

The default password for the server control functions is `retsam`.
The username is always `master` and cannot be changed.

If you want to override this, you have to use a system property when launching the application.
`java -DmasterPassword=secret -jar mazegame-server-ktor-<version>-all.jar`

### Launch the server with custom bots in the classpath

In order do make your own bots available to the server (such as a custom frenzy bot), you need to start the server with
the jars of your bots in the classpath.
This is one way to do it.

- Create a directory named `bots` in the same directory, where the jar and the `config` directory is located.
- Move the jar(s) of your bot(s) into this directory.
    - Use the "small jars", containing only your classes, not the fat jars that might also be created, otherwise you
      will end up with redundancies in the classpath. In order for server-sided bots to work, the server requires the
      headless part of the client in the classpath. This is also included in the fat jars of your bot(s).
- Launch the application using the following command:

`java -cp ".\mazegame-server-ktor-<version>-all.jar;.\bots\*" de.dreamcube.mazegame.server.ApplicationKt` (Windows)

`java -cp "./mazegame-server-ktor-<version>-all.jar:./bots/*" de.dreamcube.mazegame.server.ApplicationKt` (Linux/Mac)

Your bots will be found with reflection.
Your log should contain something along those lines:

```
Reflections took 1050 ms to scan 3 urls, producing 2838 keys and 22360 values
Strategy 'dummy' will be registered for class 'de.dreamcube.mazegame.client.maze.strategy.Aimless'.
Strategy 'ninja_ng' will be registered for class 'de.dreamcube.mazegame.client.bots.ninja_ng.NinjaStrategy'.
Strategy 'chopstick' will be registered for class 'de.dreamcube.mazegame.client.bots.chopstick.ChopstickStrategy'.
Strategy 'stubborn' will be registered for class 'de.dreamcube.mazegame.client.bots.stubborn.Stubborn'.
Strategy 'trapeater' will be registered for class 'de.dreamcube.mazegame.client.maze.strategy.Trapeater'.
Strategy 'planestrider' will be registered for class 'de.dreamcube.mazegame.client.bots.advanced.impl.PlainStrategy'.
Strategy 'underdog' will be registered for class 'de.dreamcube.mazegame.client.bots.advanced.impl.Underdog'.
```

If only `trapeater` and `dummy` are listed, your classpath is incorrect or your jar does not contain your bot(s).

All the above `-D` alterations are also possible with this launch method.
Just put the `-D` options right before the main class is called and you will be fine.