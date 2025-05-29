package mazegame.server_ktor.maze.server_bots

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mazegame.server_ktor.maze.MazeServer

/**
 * Abstraction of server-side bots that can only be spawned once, such as the trapeater or the frenzy bot.
 */
abstract class ServerBotHandler(protected val mazeServer: MazeServer) {

    /**
     * Server-side client including the bot.
     */
    internal var client: ServerSideClient? = null

    /**
     * Tells, if the bot is running.
     */
    val active: Boolean
        get() = client != null

    abstract val botAlias: String

    protected val mutex: Mutex = Mutex()

    /**
     * Checks, if the bot should spawn or despawn and should do so. Can also perform other adjustments associated with it.
     */
    abstract suspend fun handle()

    /**
     * Spawns the bot if it is not already spawned.
     */
    internal suspend fun spawn(associateInBackground: Boolean = true) {
        mutex.withLock {
            if (client == null) {
                client = mazeServer.internalSpawnServerSideBot(botAlias)
                if (associateInBackground) {
                    mazeServer.associateBotWithClientConnectionInTheBackground(client)
                } else {
                    mazeServer.associateBotWithClientConnection(client)
                }
                postSpawn()
            }
        }
    }

    /**
     * Enables decisions after the bot has spawned.
     */
    protected abstract suspend fun postSpawn()

    /**
     * Despawns the bot if it is spawned.
     */
    internal suspend fun despawn() {
        mazeServer.getClientConnection(client?.clientId)?.stop()
        mutex.withLock { client = null }
        postDespawn()
    }

    /**
     * Enables decisions after the bot has despawned
     */
    protected abstract suspend fun postDespawn()

}