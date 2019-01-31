package no.nav.syfo

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.forskuttering.ForskutteringsClient
import no.nav.syfo.forskuttering.registrerForskutteringApi
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

fun main(args: Array<String>) = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val env = getEnvironment()
    val applicationState = ApplicationState()
    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        initRouting(applicationState, env)
    }.start(wait = false)

    try {
        val listeners = (1..env.applicationThreads).map {
            launch {
                blockingApplicationLogic(applicationState)
            }
        }.toList()

        runBlocking {
            Runtime.getRuntime().addShutdownHook(Thread {
                applicationServer.stop(10, 10, TimeUnit.SECONDS)
            })

            applicationState.initialized = true
            listeners.forEach { it.join() }
        }
    } finally {
        applicationState.running = false
    }
}

suspend fun blockingApplicationLogic(applicationState: ApplicationState) {
    while (applicationState.running) {
        delay(100)
    }
}

@KtorExperimentalAPI
fun Application.initRouting(applicationState: ApplicationState, env: Environment) {
    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }
    val accessTokenClient = AccessTokenClient(env.aadAccessTokenUrl, env.credentials.clientid, env.credentials.clientsecret, httpClient)
    val forskutteringsClient = ForskutteringsClient(env.servicestranglerUrl, accessTokenClient, httpClient)
    routing {
        registerNaisApi(
                readynessCheck = {
                    applicationState.initialized
                },
                livenessCheck = {
                    applicationState.running
                }
        )
        registrerForskutteringApi(forskutteringsClient)
    }
}
