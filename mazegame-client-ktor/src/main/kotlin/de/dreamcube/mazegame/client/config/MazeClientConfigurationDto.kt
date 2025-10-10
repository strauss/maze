package de.dreamcube.mazegame.client.config

class MazeClientConfigurationDto @JvmOverloads constructor(
    val serverAddress: String,
    val serverPort: Int,
    val strategyName: String,
    val displayName: String = strategyName
)