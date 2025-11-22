package de.dreamcube.mazegame.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.dreamcube.mazegame.common.api.MazeServerConfigurationDto
import de.dreamcube.mazegame.server.control.ServerController
import de.dreamcube.mazegame.server.control.configureAuthentication
import de.dreamcube.mazegame.server.control.configureRouting
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.thymeleaf.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.io.File

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

private const val MAZEGAME_CONFIG_PROPERTY = "mazegame.config-path"
private const val MANUAL_SERVER_START_NOTIFICATION = "Servers have to be started manually using the REST interface."

private val LOGGER = LoggerFactory.getLogger(Application::class.java)


fun Application.module() {
    val configPath: String? =
        System.getProperty(MAZEGAME_CONFIG_PROPERTY) ?: environment.config.propertyOrNull(MAZEGAME_CONFIG_PROPERTY)
            ?.getString()

    val objectMapper = ObjectMapper(YAMLFactory())
    objectMapper.registerKotlinModule()
    val serverList: List<MazeServerConfigurationDto> = if (configPath == null) {
        LOGGER.warn("No configuration file specified. $MANUAL_SERVER_START_NOTIFICATION")
        listOf()
    } else {
        val configurationFile = File(configPath)
        if (configurationFile.exists()) {
            try {
                objectMapper.readValue(configurationFile)
            } catch (ex: Exception) {
                LOGGER.error(
                    "Error while parsing the configuration file '$configurationFile' $MANUAL_SERVER_START_NOTIFICATION The error: '${ex.message}'."
                )
                listOf()
            }
        } else {
            LOGGER.warn("Configuration file '$configurationFile' does not exist. $MANUAL_SERVER_START_NOTIFICATION")
            listOf()
        }
    }

    if (serverList.isNotEmpty()) {
        serverList.forEach {
            val result = ServerController.launchServer(it)
            runBlocking {
                try {
                    result.await()
                } catch (ex: Throwable) {
                    LOGGER.error("Failed to start server on port '${it.connection.port}': ", ex)
                }
            }
        }
    } else {
        LOGGER.info("No servers configured.")
    }

    monitor.subscribe(ApplicationStopping) {
        ServerController.quitAllMazeServers()
    }
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }
    configureAuthentication()
    configureRouting()
}
