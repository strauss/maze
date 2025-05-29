package mazegame.server_ktor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.thymeleaf.*
import kotlinx.coroutines.runBlocking
import mazegame.server_ktor.config.MazeServerConfigurationDto
import mazegame.server_ktor.control.ServerController
import mazegame.server_ktor.control.configureAuthentication
import mazegame.server_ktor.control.configureRouting
import org.slf4j.LoggerFactory
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.io.File

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

private val LOGGER = LoggerFactory.getLogger(Application::class.java)

fun Application.module() {
    val configPath: String = environment.config.property("mazegame.config-path").getString()

    val objectMapper = ObjectMapper(YAMLFactory())
    objectMapper.registerKotlinModule()
    val serverList: List<MazeServerConfigurationDto> = objectMapper.readValue(File(configPath))

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
