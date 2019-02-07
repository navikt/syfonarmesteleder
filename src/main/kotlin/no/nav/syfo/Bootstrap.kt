package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.forskuttering.ForskutteringsClient
import no.nav.syfo.forskuttering.registrerForskutteringApi
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import java.net.ProxySelector
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)
private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun main(args: Array<String>) = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val env = getEnvironment()
    val authorizedUsers = listOf(env.syfosoknadId)
    val applicationState = ApplicationState()
    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        val jwkProvider = JwkProviderBuilder(URL(env.jwkKeysUrl))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        install(Authentication) {
            jwt {
                verifier(jwkProvider, env.jwtIssuer)
                realm = "Syfonarmesteleder"
                validate { credentials ->
                    val appid: String = credentials.payload.getClaim("appid").asString()
                    log.info("authorization attempt for $appid")
                    if (appid in authorizedUsers && credentials.payload.audience.contains(env.clientid)) {
                        log.info("authorization ok")
                        return@validate JWTPrincipal(credentials.payload)
                    }
                    log.info("authorization failed")
                    return@validate null
                }
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

fun Application.initRouting(applicationState: ApplicationState, env: Environment) {
    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
    }
    val accessTokenClient = AccessTokenClient(env.aadAccessTokenUrl, env.clientid, env.credentials.clientsecret, httpClient)
    val forskutteringsClient = ForskutteringsClient(env.servicestranglerUrl, env.servicestranglerId, accessTokenClient, httpClient)
    routing {
        registerNaisApi(
                readynessCheck = {
                    applicationState.initialized
                },
                livenessCheck = {
                    applicationState.running
                }
        )
        authenticate {
            registrerForskutteringApi(forskutteringsClient)
        }
    }
}
