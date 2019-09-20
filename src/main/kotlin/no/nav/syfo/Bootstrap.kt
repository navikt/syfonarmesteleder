package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.db.Database
import no.nav.syfo.db.VaultCredentialService
import no.nav.syfo.forskuttering.ForskutteringsClient
import no.nav.syfo.forskuttering.registrerForskutteringApi
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.narmestelederapi.NarmesteLederClient
import no.nav.syfo.narmestelederapi.registrerNarmesteLederApi
import no.nav.syfo.syfoservice.NarmesteLederDTO
import no.nav.syfo.syfoservice.leggTilForskutteringer
import no.nav.syfo.syfoservice.leggTilNarmesteLedere
import no.nav.syfo.syfoservice.toForskutteringDAO
import no.nav.syfo.syfoservice.toNarmesteLederDAO
import no.nav.syfo.vault.Vault
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.net.ProxySelector
import java.net.URL
import java.time.Duration
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

fun main() = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val env = getEnvironment()
    val authorizedUsers = listOf(env.syfosoknadId, env.syfovarselId, env.arbeidsgivertilgangId)
    val applicationState = ApplicationState()

    val consumerProperties = loadBaseConfig(env).toConsumerConfig()

    val vaultCredentialService = VaultCredentialService()
    val database = Database(env, vaultCredentialService)

    launch(backgroundTasksContext) {
        try {
            Vault.renewVaultTokenTask(applicationState)
        } finally {
            applicationState.running = false
        }
    }

    launch(backgroundTasksContext) {
        try {
            vaultCredentialService.runRenewCredentialsTask { applicationState.running }
        } finally {
            applicationState.running = false
        }
    }

    launchListeners(
        env,
        applicationState,
        database,
        consumerProperties
    )

    embeddedServer(Netty, env.applicationPort) {
        val jwkProvider = JwkProviderBuilder(URL(env.jwkKeysUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        install(MicrometerMetrics) {
            registry =
                PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)
            meterBinders = listOf(
                ClassLoaderMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                JvmThreadMetrics(),
                LogbackMetrics()
            )
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

    Runtime.getRuntime().addShutdownHook(Thread {
        coroutineContext.cancelChildren()
    })

    applicationState.initialized = true
}


fun CoroutineScope.createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    launch {
        try {
            action()
        } finally {
            applicationState.running = false
        }
    }

suspend fun blockingApplicationLogicRecievedNarmesteLeder(
    applicationState: ApplicationState,
    kafkaconsumer: KafkaConsumer<String, String>,
    database: Database
) {
    while (applicationState.running) {
        val narmesteLedere: List<NarmesteLederDTO> = kafkaconsumer
            .poll(Duration.ofMillis(0))
            .map { objectMapper.readValue<NarmesteLederDTO>(it.value()) }
        database.leggTilForskutteringer(narmesteLedere.map { it.toForskutteringDAO() })
        database.leggTilNarmesteLedere(narmesteLedere.map { it.toNarmesteLederDAO() })
        log.info("Lagret ${narmesteLedere.size} nærmeste ledere")
    }
    delay(100)
}

fun CoroutineScope.launchListeners(
    env: Environment,
    applicationState: ApplicationState,
    database: Database,
    consumerProperties: Properties
) {
    val narmesteLederTopic = 0.until(env.applicationThreads).map {
        val kafkaconsumernarmesteLeder = KafkaConsumer<String, String>(consumerProperties)

        kafkaconsumernarmesteLeder.subscribe(listOf("helse-narmesteLeder-v1"))

        createListener(applicationState) {
            blockingApplicationLogicRecievedNarmesteLeder(applicationState, kafkaconsumernarmesteLeder, database)
        }
    }.toList()

    applicationState.initialized = true
    runBlocking { narmesteLederTopic.forEach { it.join() } }
}

fun Application.initRouting(applicationState: ApplicationState, env: Environment) {
    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    val proxyConfig: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        config()
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
    }

    val httpClientWithProxy = HttpClient(Apache, proxyConfig)
    val httpClient = HttpClient(Apache, config)

    val accessTokenClient =
        AccessTokenClient(env.aadAccessTokenUrl, env.clientid, env.credentials.clientsecret, httpClientWithProxy)
    val forskutteringsClient =
        ForskutteringsClient(env.servicestranglerUrl, env.servicestranglerId, accessTokenClient, httpClient)
    val narmesteLederClient =
        NarmesteLederClient(env.servicestranglerUrl, env.servicestranglerId, accessTokenClient, httpClient)

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
            registrerNarmesteLederApi(narmesteLederClient)
        }
    }
}
