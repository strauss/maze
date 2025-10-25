package de.dreamcube.mazegame.client.maze.strategy

/**
 * Used to annotate a subclass of [Strategy] and mark it as a bot strategy.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Bot(
    val value: String,
    val isSpectator: Boolean = false,
    val isHuman: Boolean = false,
    val flavor: String = "My creator did not figure out (yet), how to change this text :-)"
)
