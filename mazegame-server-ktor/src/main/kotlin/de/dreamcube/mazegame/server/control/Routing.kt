package de.dreamcube.mazegame.server.control

import de.dreamcube.mazegame.common.maze.BaitType
import de.dreamcube.mazegame.server.config.MazeServerConfigurationDto
import de.dreamcube.mazegame.server.contest.ContestConfiguration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*

private const val basicAuth: String = "basic"
private const val bearerAuth: String = "bearer"
private const val authRealm: String = "Maze Server Control"
private const val jwtTtl: Long = 900L
private const val pathServerId: String = "serverId"
private const val queryNow: String = "now"
private const val queryStop: String = "stop"
private const val pathPlayerId: String = "playerId"
private const val pathNick: String = "nick"

fun Application.configureAuthentication() {
    authentication {
        basic(name = basicAuth) {
            realm = authRealm
            validate { credentials ->
                if (UserService.checkCredentials(credentials.name, credentials.password)) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }

        bearer(name = bearerAuth) {
            realm = authRealm
            authenticate { tokenCredential ->
                JwtService.verify(tokenCredential.token)?.let { jwt ->
                    UserIdPrincipal(jwt.subject)
                }
            }
        }
    }
}

fun Application.configureRouting() {
    routing {

        /* TODO: this does not work properly yet and is therefore deactivated
        staticResources("/static", "static")

        get("/index") {
            call.respond(ThymeleafContent("index", mapOf()))
        }

        // TODO: This has to be inside the bearer auth or protected by a cookie
        get("/hello") {
            call.respond(ThymeleafContent("hello", mapOf()))
        }
        */

        /**
         * This endpoint is used for allowing a client to get essential connection information about all available running maze servers.
         */
        get("server") {
            ServerController.getReducedServerInformation(call)
        }

        authenticate(basicAuth) {
            post("/login") {
                val user: UserIdPrincipal? = call.principal<UserIdPrincipal>()
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                val token = JwtService.issue(user.name, jwtTtl)
                call.respond(mapOf("token" to token, "expires" to jwtTtl))
            }
        }

        authenticate(bearerAuth) {
            post("/server/{$pathServerId}/control/go") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.go(call, serverId)
            }

            post("/server/{$pathServerId}/control/stop") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                val now: Boolean? = call.getBooleanQueryParameter(queryNow)
                if (now == null) {
                    ServerController.stop(call, serverId)
                } else {
                    ServerController.stop(call, serverId, now)
                }
            }

            post("/server/{$pathServerId}/control/quit") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.quitSingleMazeServer(call, serverId)
            }

            post("/server") {
                val serverConfiguration: MazeServerConfigurationDto = call.receive<MazeServerConfigurationDto>()
                ServerController.launchNewMazeServer(call, serverConfiguration)
            }

            post("/server/{$pathServerId}/control/clear") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                val stop: Boolean? = call.getBooleanQueryParameter(queryStop)
                if (stop == null) {
                    ServerController.clear(call, serverId)
                } else {
                    ServerController.clear(call, serverId, stop)
                }
            }

            post("/server/{$pathServerId}/control/all-food") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.transformBaits(call, serverId, BaitType.FOOD)
            }

            post("/server/{$pathServerId}/control/all-coffee") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.transformBaits(call, serverId, BaitType.COFFEE)
            }

            post("/server/{$pathServerId}/control/all-gems") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.transformBaits(call, serverId, BaitType.GEM)
            }

            post("/server/{$pathServerId}/control/all-traps") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.transformBaits(call, serverId, BaitType.TRAP)
            }

            post("/server/{$pathServerId}/control/bait-rush") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.baitRush(call, serverId)
            }

            post("/server/{$pathServerId}/control/spawn/{$pathNick}") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                val nick: String = call.getStringParameter(pathNick) ?: return@post
                ServerController.spawnBot(call, serverId, nick)
            }

            post("/server/{$pathServerId}/control/kill/{$pathPlayerId}") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                val playerId: Int = call.getIntParameter(pathPlayerId) ?: return@post
                ServerController.kill(call, serverId, playerId)
            }

            post("/server/{$pathServerId}/control/teleport") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                val teleportCommand: TeleportCommandDto = call.receive<TeleportCommandDto>()
                ServerController.teleportPlayer(call, serverId, teleportCommand)
            }

            post("/server/{$pathServerId}/control/put-bait") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                val putBaitCommand: PutBaitCommandDto = call.receive<PutBaitCommandDto>()
                ServerController.putBait(call, serverId, putBaitCommand)
            }

            get("/server/{$pathServerId}/contest") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@get
                ServerController.getContestInformation(call, serverId)
            }

            post("/server/{$pathServerId}/contest") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                val contestConfiguration: ContestConfiguration = call.receiveNullable() ?: ContestConfiguration()
                ServerController.startContest(call, serverId, contestConfiguration)
            }

            delete("/server/{$pathServerId}/contest") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@delete
                ServerController.stopContest(call, serverId)
            }

            post("/server/{$pathServerId}/contest/report") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@post
                ServerController.triggerReport(call, serverId)
            }

            get("/server/{$pathServerId}/info") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@get
                ServerController.getServerInformation(call, serverId)
            }

            get("/server/{$pathServerId}/info/config") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@get
                ServerController.getServerConfiguration(call, serverId)
            }

            get("/server/{$pathServerId}/info/player") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@get
                ServerController.getPlayerInformation(call, serverId)
            }

            get("/server/{$pathServerId}/info/player/{$pathPlayerId}") {
                val serverId: Int = call.getIntParameter(pathServerId) ?: return@get
                val playerId: Int = call.getIntParameter(pathPlayerId) ?: return@get
                ServerController.getPlayerInformation(call, serverId, playerId)
            }
        }
    }
}

@Suppress("kotlin:S6312")
private suspend fun ApplicationCall.getIntParameter(parameterName: String): Int? {
    val parameterRawValue: String? = parameters[parameterName]
    val parameterValue: Int? = parameterRawValue?.toIntOrNull()
    if (parameterValue == null) {
        val errorMessage: String = if (parameterRawValue == null)
            "Path parameter '$parameterName' is missing."
        else
            "Path parameter '$parameterName' must be an integer."
        respond(HttpStatusCode.BadRequest, errorMessage)
        return null
    }
    return parameterValue
}

@Suppress("kotlin:S6312")
private suspend fun ApplicationCall.getStringParameter(parameterName: String): String? {
    val parameterValue: String? = parameters[parameterName]
    if (parameterValue == null) {
        respond(HttpStatusCode.BadRequest, "Path parameter '$parameterName' is missing.")
    }
    return parameterValue
}

private fun ApplicationCall.getBooleanQueryParameter(parameterName: String): Boolean? {
    val parameterRawValue: String? = request.queryParameters[parameterName]
    if (parameterRawValue == null) {
        return null
    }
    val parameterToLower: String = parameterRawValue.toLowerCasePreservingASCIIRules()
    if (parameterToLower == "true" || parameterToLower == "yes") {
        return true
    } else if (parameterToLower == "false" || parameterToLower == "no") {
        return false
    }
    return null
}

