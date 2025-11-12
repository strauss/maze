package de.dreamcube.mazegame.client.config

/**
 * Simple configuration object for the maze client.
 */
class MazeClientConfigurationDto @JvmOverloads constructor(
    /**
     * The server address.
     */
    val serverAddress: String,

    /**
     * The server port.
     */
    val serverPort: Int,

    /**
     * The name of the strategy.
     */
    val strategyName: String,

    /**
     * Should the flavor text be emitted to the server? Default is true.
     */
    val withFlavor: Boolean = true,

    /**
     * The actual nickname to be displayed. Default is [strategyName].
     */
    val displayName: String = strategyName
)