package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.AccessTokenClient
import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.metrics.monitorHttpRequests
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.forskuttering.ForskutteringsClient
import no.nav.syfo.forskuttering.registrerForskutteringApi
import no.nav.syfo.log
import no.nav.syfo.narmesteleder.NarmesteLederClient
import no.nav.syfo.narmesteleder.UtvidetNarmesteLederService
import no.nav.syfo.narmesteleder.registrerNarmesteLederApi
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.service.PdlPersonService
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector
import java.util.UUID

@KtorExperimentalAPI
fun createApplicationEngine(
    env: Environment,
    applicationState: ApplicationState,
    vaultSecrets: VaultSecrets,
    jwkProvider: JwkProvider
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        setupAuth(jwkProvider, env, vaultSecrets)
        install(CallId) {
            generate { UUID.randomUUID().toString() }
            verify { callId: String -> callId.isNotEmpty() }
            header(HttpHeaders.XCorrelationId)
        }
        install(StatusPages) {
            exception<Throwable> { cause ->
                call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
                log.error("Caught exception", cause)
            }
        }

        val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
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

        val stsOidcClient = StsOidcClient(
            username = vaultSecrets.serviceuserUsername,
            password = vaultSecrets.serviceuserPassword,
            stsUrl = env.stsUrl
        )
        val pdlClient = PdlClient(
            httpClient,
            env.pdlGraphqlPath,
            PdlClient::class.java.getResource("/graphql/getPerson.graphql").readText().replace(Regex("[\n\t]"), "")
        )
        val pdlPersonService = PdlPersonService(pdlClient, stsOidcClient)

        val accessTokenClient =
            AccessTokenClient(env.aadAccessTokenUrl, vaultSecrets.clientId, vaultSecrets.clientSecret, httpClientWithProxy)
        val forskutteringsClient =
            ForskutteringsClient(env.servicestranglerUrl, env.servicestranglerId, accessTokenClient, httpClient)
        val narmesteLederClient =
            NarmesteLederClient(env.servicestranglerUrl, env.servicestranglerId, accessTokenClient, httpClient)
        val utvidetNarmesteLederService = UtvidetNarmesteLederService(narmesteLederClient, pdlPersonService)

        routing {
            registerNaisApi(applicationState)
            authenticate {
                registrerForskutteringApi(forskutteringsClient)
                registrerNarmesteLederApi(narmesteLederClient, utvidetNarmesteLederService)
            }
        }
        intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
    }
