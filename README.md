# The Maze Game

Welcome to this amazing maze game.
This project is designed to be educational and entertaining at the same time.
Here you find all you need for giving people, who want to learn either Java or Kotlin, a challenging exercise.

The objective is creating an automated bot that can navigate through a randomly generated maze and collect items
(so-called "baits").
There are different kinds of baits with different score values.
The bots need to maximize their score in comparison to the other players in the maze.
Bots are required to make "good" decisions on which bait they will go for next.

## Latest version

[![Release](https://jitpack.io/v/de.dreamcube.maze/Repo.svg)](https://jitpack.io/#de.dreamcube/maze)

## Building the project

This project uses Gradle.
If you clone it into InteliJ, you don't have to think about it too much.
The top-level task "shadow/shadowJar" will build everything and create complete jars into the `build` directories of
your project.

If you want to build it, using the command line, just run the following command:

`./gradlew clean shadowJar`

The `clean` is optional, it just ensures a build from scratch.

In order to launch the server without the IDE, building the project is essential.
This is the command that gives you the server jar.
If you want to know how to configure and launch it, have a look at the
[server documentation](mazegame-server-ktor/README.md).

If you want to learn how to set dependencies to this project, scroll down to [Bots](#bots).
But you might want to learn about the game first :-)

## Game rules

When entering the game, a player is placed randomly in the maze.

There are three different moves a player can make.

- "step" just steps forward
- "turn left" turns the player counterclockwise
- "turn right" turns the player clockwise

The player is only allowed to make a move, when it gets a signal to do so.
Those signals will be sent out at fixed intervals.
The default interval is "150ms", but it can be different and also change at any time.

When two players collide, they both get teleported to random locations on the map.
Depending on the configured rules, the causing player might lose a random bait (it will "fall down" at the "crash site")
and the associated points.

There are four types of baits that can be collected:

- "food" for 13 points
- "coffee" for 42 points
- "gem" for 314 points
- "trap" for -128 points

When collecting either of them, the player gains (or loses) the amount of points and a new bait is randomly generated
and randomly placed on the map.
Baits can be invisible.
However, they will appear after a while.
When "collecting" a trap, the player also gets teleported randomly to a different location on the map.

### Game events

During the game, certain events can occur (if that behavior is configured).

- All baits can transform into the same bait type (including traps)
- More baits than usual can appear (bait rush)
- Special bots can spawn and disrupt the game
- The game speed can change
- Baits can vanish and regenerate

## The components

The game is split into a [server](mazegame-server-ktor) and several [clients](mazegame-client-ktor).
The server does the "heavy lifting".
It generates the map, keeps track of and synchronizes all player movements, generates and distributes the baits, keeps
track of all scores, and several more things.
The server is even capable of launching its own clients, which is useful for bots that can act as obstacles (dummy) or
for cleaning up (trapeater).

If you actually want to see something, you might want to use the [client-ui](mazegame-ui).
It allows you to set up your connection with the server, visualizes the map, and it also allows for controlling the
server, if you know the server password.

The UI project also includes some "special strategies".

- Spectator: When selecting this strategy, you will just spectate the game, but not participate in it
    - For direct connections, you have to know the spectator nickname. Managed connections handle this automatically. In
      both cases, this has to be enabled in the server.
- Human Players: Gives you manual control over the player
    - First person: The arrow keys for up, left and right directly translate to the matching commands.
    - Third person: All four arrow keys are used for movement. The player will move in the direction associated with the
      key and determine the correct commands automatically.
    - "Click and Collect" (no, this project is not sponsored by Ikea!): You control your player with the mouse. Just
      click on the bait you like, and the player (almost a bot) will collect it for you (or run into a trap or another
      player if one of them is in the way). You can only select baits in this mode.

The client also ships with two predefined "strategies".

- "dummy": The classical dummy bot. It moves randomly and reacts to wall crashes if it tried to run into a wall.
- "trapeater": This strategy is designed to clean up the traps on the field. It is intended to be run inside the server.
  It uses the "stubborn" approach of selecting the nearest trap by "manhattan distance" and searching with the "A star
  search" the best path to it. Other baits and other players are ignored and not avoided. It will follow the path until
  the targeted trap vanishes, or it is teleported away for any reason (collision, invisible trap).

## Bots

In order to create your own bot, you need a distinct project with dependencies to at least the client and the common
module.

### The template repository

The easiest way to achieve this is using [this repository](https://github.com/strauss/maze-bots-template) as template
for your own new repository.
It gives you a fully configured Gradle multi-module project, that allows you to develop your own bots in Java and
Kotlin.
It also contains a convenience wrapper for launching the UI containing your bots.

### Setting up the dependencies manually

You need to add Jitpack as repository and include the dependency to at least the client.

The correct `${version}` can be found [above](#latest-version).

In both cases, you want to have a main function/method for calling the UI.
[Peek into the template repository](https://github.com/strauss/maze-bots-template/blob/main/ui-wrapper/src/main/java/de/dreamcube/mazegame/UiLauncher.java)
on how this is done.

#### Gradle

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("de.dreamcube.maze:mazegame-client-ktor:${version}")
    implementation("de.dreamcube.maze:mazegame-common:${version}")
}
```

#### Maven

Be warned, Jitpack will not work everywhere.
It highly depends on your Maven settings.
If you do not want to deal with that, just stick to Gradle or fork the template repository,
[as described above](#the-template-repository).

```xml

<project>
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>de.dreamcube.maze</groupId>
            <artifactId>mazegame-client-ktor</artifactId>
            <version>${version}</version>
        </dependency>
        <dependency>
            <groupId>de.dreamcube.maze</groupId>
            <artifactId>mazegame-common</artifactId>
            <version>${version}</version>
        </dependency>
    </dependencies>
</project>
```

### Bot development

In order to actually develop your own bot, consult the [bot documentation](mazegame-client-ktor/doc/bot.md) and the
[bot ui documentation](mazegame-client-ktor/doc/bot_ui.md) for further details.

If you already developed a bot for the "old client", you should consider reading
the [bot migration documentation](mazegame-client-ktor/doc/bot_migration.md) if you want to incorporate it into the new
client.

## History

The maze game goes back to, what I would call, the "classical maze game".
It was developed by Dr. Volker Riediger at the University of Koblenz in Germany.
I learned about it around 2008, when I wrote my very first client and bot as exercise for the lecture "Programmierung"
(programming).

*Fun fact: My very own first bot "snitch" only got me 10 out of 12 points for the exercise :-D*

As a grand finale, Volker always hosted a contest event, where the student groups could compete against his very own
famous "ninja" bot.
My semester seemed to be the last one, that was blessed with this amazing task.

Volker was kind enough, to hand out the source code of his server application and his client application, containing his
"ninja" bot.
This code base rested for a very long time on my hard drive and collected digital dust.

Time went by.
I graduated as one of the last people in Germany who got a Diploma in CS instead of a Master's degree.
I went through my first jobs and almost forgot about the maze game.

At my current job, I regularly host a Java course for our apprentices ("Azubis").
When I started it in 2023, I was searching for a final project for the apprentices, that is both challenging and
entertaining.
I rediscovered the maze game and began to polish it up for my apprentices.

I took the server as it was and tweaked the client, such that it was easier to implement a bot.
At university, the exercises required you to implement a whole client application.
That would have been too much of a project for my course.
Therefore, I limited it to "only" implementing a bot for the game.
I also hosted a contest at the end of the course.

The apprentices loved it and I got hooked on the maze game again.
I implemented my own new bots at that time.
It didn't stop there.

I extended the server with new features (such as teleporting players and delay compensation) and made the client more
accessible.
At some point I realized something.
I wanted to make this amazing project available for more people to enjoy.
My plan was making the code "open source", which is what I eventually did.

I didn't want to release the code as it was.
For one, it was not my code, it was Volker's.
He might have given it to us students, but it just felt completely wrong.

I also wanted to try something "new".
My favorite programming language changed from "Java" to "Kotlin" around 2019.
At my current job I cannot use that language very often, but I still wanted to experiment with its capabilities.
So my idea was rewriting the server based on the Ktor framework.
This would force me to also learn about the concept of coroutines, which I wanted to do since 2019 and never found the
time or opportunity for.

In April 2025, I started the development of the new server, while the "third season" of the maze game was running.
At that time, the apprentices still got the "old server" for practicing.
For the contest, I managed to have a stable version of the new server.

In that condition, the server still relied on the old client for "server-sided" bots, such as the trapeater.
I couldn't release it yet.
The client was the next thing to replace.
I wanted to separate the core client (protocol, networking, and strategy management) from the UI.
For the client, I also chose Kotlin as language and some parts of it also use Ktor elements.

The UI was a tougher choice.
I didn't choose Java-FX, because outside of universities, it seems insignificant.
Also: I learned, that Volker revived the maze game for a different lecture.
He demands Java-FX and I don't want to release something that might simplify the work for his students.
They have to learn it the hard way :-)
I didn't want to move to "Compose Multiplatform" (yet), because it would have been completely new for me, and I am
actually a backend developer :-)
So I combined my knowledge of Swing with my desire for Kotlin and created a Kotlin-Swing-UI.
But don't worry, I gave it a "more modern" look and feel (FlatLaf).

I started developing the new client and the new UI in October 2025.
Volker was kind enough to allow me releasing my adaptation of his original project.
If you can read this, I was successful and released the whole thing as an open source project for everyone.

## What about the bots?

In this project, the "more advanced" bots are intentionally excluded.
I migrated (almost) all of my own bots to the new client (some remained in Java, most moved to Kotlin).
I also migrated Volker's "ninja" not only to the new client, but also to Kotlin.
Its new version is now called "ninja_ng".

Here are some facts about the advanced bots that might inspire your own approaches.

### "ninja" (now "ninja_ng")

Volker once described his algorithm as "spill water".
The bot treats other players as walls, its evaluation function is outstanding.
However, the bot's performance gets worse, the more other players compete.

I still use this bot for evaluating the quality of my apprentice's bots.
If they can compete with "ninja" (or even surpass him), I consider them "very good".

### "chopstick"

My first "new" bot from 2023.
It searches "backwards" and only considers baits that can be reached before any other player can reach it.
The paths are not optimal, but it still performs pretty well, if the maze is not too crowded.

The name is some kind of "inside joke" from the first iteration of the Java course.
For the last session I implemented a non-thread-safe version of the philosopher's problem.
The apprentice's exercise was "make it thread-safe".
In my approach the chopsticks "knew" more about the philosophers than the other way around (it seemed practical from a
technical point of view).
One of the apprentices was too confused by that fact, that they couldn't finish the exercise.

I named the bot "chopstick" because it does things unconventionally backwards.
It was the first bot I migrated to the new client.
It is also one of the bots that remained implemented in Java.
Its flavor text is "The chopsticks in your hand think you should start eating now!"

### "bloodhound"

This was my second bot from 2023.
It does not search at all and does not lock on targets.
Besides that, it is still able to compete with other bots, if the maze is not too crowded.

Its name should suggest how it works.
But be warned, that approach has its own rabbit hole of complications.
You might want to implement a search instead :-)

This bot has not been migrated to the new client yet.
I am also not sure if I will port it to Kotlin or not.

### "underdog"

This bot was my very first bot from 2023, that I would consider "very good".
It uses a "complicated" map structure for avoiding too expensive search algorithms.
It searches forwards (as all bots should do :-) ).
It uses a technique, that one of my former apprentices called "mind reading" (they actually named their own bot
"...mindreader" and won the first contest with it).

I hacked down the first version in 2 hours (mostly, because I already implemented the map structure for "chopstick"
without using it there).
Therefore, the name "underdog", because "No one expected me, but here I am!" (flavor text).
The first version was not thread-safe but still performed pretty well.
In 2024, I redesigned it to be very thread-safe and added more tweaks to it.

The final tweak ultimately created "mindripper", my best bot (yet).

"underdog" is already migrated to the new client and has been ported to Kotlin and now uses coroutines.
I hope he is still thread-safe :-)

### "mindripper"

"underdog" and "mindripper" are based on the same core.
"mindripper" goes beyond simple mind reading.
It puts a lot of effort into it, but the difference between "underdog" and "mindripper" is almost neglectable (around
+0.6% better in the long run).
However, the techniques are pretty interesting and I will use that bot to further improve the strategy.

"mindripper" is not migrated yet, but I just postponed it to after I released this project.

### "planewalker"

This is just an "underdog" without the mindreading ... or a "chopstick" that searches forwards ... or my very own
"ninja", but not that "cool".

It is actually a "new" bot, based on, what I now call, my "advanced bot family".
In fact, "chopstick", "planewalker", "underdog", and "mindripper" all use the very same evaluation function.
"chopstick" would then be the "black sheep" of the family, because it uses its own maze structure and is implemented in
Java.

### "stubborn"

This bot is more of a joke.
It is basically a "trapeater" that is actually searching for baits.
Once it locks on a target, it holds on to it, ignoring gems that might spawn behind it.

For evaluating the bot quality, it's good to have a "bad strategy" at hand.
Ironically, I gave it the same evaluation function, as my advanced family uses.

If you analyzed the code of
[trapeater](mazegame-client-ktor/src/main/kotlin/de/dreamcube/mazegame/client/maze/strategy/Trapeater.kt), you could
easily come up with your own stubborn implementation.

If you are one of my apprentices, I highly recommend not to do that :-)

## What about Chat-GPT

If you plan on using Chat-GPT for bot development ... well depending on how you will approach it, I would say "go for
it!".
Nowadays, using an AI as a tool is not a "sin" (anymore).

However, I highly recommend to discuss topics on "a higher level".
Don't let the AI do the coding for you.
The goal should be "learning to code".
If you struggle with certain aspects of your code, let the AI explain it to you.

Out of curiosity, I once let Chat-GPT develop a bot on its own.
It wasn't able to just solve the problem.
I had to guide it towards a feasible solution.
That was a fun experience, because we swapped roles.
Usually the AI tells the human how to approach a problem.
This time it was the other way around.

The AI produced several bots of different quality.
I would consider the best of its bots to be of "mediocre" quality.
However, I know how Chat-GPT approaches this task.
Therefore, I think I am able to spot a bot that was created or inspired by Chat-GPT.

On the other hand that was almost a year ago and the quality might have improved by now.
I might try it again with the new client.

If you are one of my apprentices, keep that in mind.
Also note, that Chat-GPT-bots will not win you the contest!

### My use of Chat-GPT in this project

When migrating and expanding on this project, Chat-GPT was a good sparring partner for discussions.
I admit, that there are a few parts in the code base, that are "highly inspired" by Chat-GPT.
One example is the class [Disposer](mazegame-common/src/main/kotlin/de/dreamcube/mazegame/common/util/Disposer.kt).

The `Disposer` was Chat-GPTs idea for a problem that I solved with a simple list.
I showed my code and asked for a better solution.
Chat-GPT came up with the `Disposer` class.
I liked the pattern and took it.

Another example is the luminance-related code in
[color_util.kt](mazegame-ui/src/main/kotlin/de/dreamcube/mazegame/ui/color_util.kt).

As mentioned above, I'm a backend developer and basically clueless, when it comes to finding the right contrast.
I am also a cis male and therefore only capable of distinguishing between 16 colors :-)

This whole construct is a preparation for a dark mode that I want to add in the future.
Without the help of Chat-GPT, the whole part with colors, contrast and luminance would be a mess and the UI would be of
worse quality.

#### My way of prompting

Usually I ask questions like "How can I select a row in a JTable using code in a way that the same events are triggered
as if I clicked on the row?".
Then Chat-GPT will give me a generic example which I can then read, understand, and adapt into my own code.
It is basically a better version of stack overflow, with the exception, that the answer will be tailored to your
problem, and you don't have to justify whatever you are doing.

Sometimes I discuss code snippets and incorporate improvement suggestions (or ignore them right away because they annoy
me).

#### Chat-GPTs biggest contribution

The most important task that Chat-GPT did for me was its help, when I migrated the former Maven project into this Gradle
project.
Without Chat-GPT, I would have failed or even given up on it entirely.

Build frameworks are a mess, no matter if we are talking about Maven or Gradle.
Everyone seems to do things differently.
Build scripts also tend to become a patchwork of copy and paste.
This is especially true for Maven projects.

When migrating the multi-module Maven project to Gradle, Chat-GPT introduced me to the
[toml file](gradle/libs.versions.toml) because I requested central version management.
I really adore this concept, because it is exactly what I desired.
This concept is even superior to the dependency management and version properties that are regularly used in Maven.

However, whenever I request a new dependency, the suggestions always use the hardcoded variants, instead of telling me
to extend the toml file and refer to it.
I learned enough to do that myself, but it really annoys me.

#### Conclusion

In a nutshell: I try to use Chat-GPT in a way that I learn from it.
I rarely copy and past its code.
Sometimes I even force it to not generate any code and just tell me the concept.

## License

Unless otherwise noted, all contents of this repository (including documentation) are licensed under the Apache License
2.0. See [LICENSE](LICENSE).
