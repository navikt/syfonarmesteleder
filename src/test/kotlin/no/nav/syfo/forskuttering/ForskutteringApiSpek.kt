package no.nav.syfo.forskuttering

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.install
import io.ktor.auth.authentication
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.http.*
import io.ktor.jackson.jackson
import io.ktor.request.uri
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.InternalAPI
import kotlinx.coroutines.io.ByteReadChannel
import no.nav.syfo.AccessTokenClient
import no.nav.syfo.ApplicationState
import no.nav.syfo.getEnvironment
import no.nav.syfo.initRouting
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Instant

const val aktorIdMedForskuttering = 123
const val aktorIdUtenForskuttering = 999
const val aktorIdMedUkjentForskuttering = 678

@InternalAPI
object ForskutteringApiSpek : Spek({
    val applicationState = ApplicationState()
    val forskutteringsClient = ForskutteringsClient("https://tjenester.nav.no", "12345", accessTokenClient, client)

    describe("Forskutteringsapi returnerer gyldig svar for gyldig request") {
        with(TestApplicationEngine()) {
            start()
            initTestAuthentication()
            application.routing {
                registrerForskutteringApi(forskutteringsClient)
            }
            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            it("Returnerer JA hvis arbeidsgiver forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktorId=$aktorIdMedForskuttering&orgnummer=333")) {
                    response.content?.shouldEqual( "{\"forskuttering\":\"JA\"}")
                }
            }
            it("Returnerer NEI hvis arbeidsgiver ikke forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktorId=$aktorIdUtenForskuttering&orgnummer=333")) {
                    response.content?.shouldEqual( "{\"forskuttering\":\"NEI\"}")
                }
            }
            it("Returnerer UKJENT hvis vi ikke vet om arbeidsgiver forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktorId=$aktorIdMedUkjentForskuttering&orgnummer=333")) {
                    response.content?.shouldEqual( "{\"forskuttering\":\"UKJENT\"}")
                }
            }
        }
    }

    describe("Forskutteringsapi returnerer BadRequest for ugyldig request") {
        with(TestApplicationEngine()) {
            start()
            initTestAuthentication()
            application.initRouting(applicationState, getEnvironment())
            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            it("Returnerer feilmelding hvis aktørid mangler") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?orgnummer=333")) {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                    response.content shouldNotEqual null
                }
            }
            it("Returnerer feilmelding hvis orgnummer mangler") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktorId=$aktorIdMedForskuttering")) {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                    response.content shouldNotEqual null
                }
            }
        }
    }
})

@InternalAPI
val client = HttpClient(MockEngine) {
    engine {
        addHandler { request ->
            when (request.url.fullUrl) {
                "https://tjenester.nav.no/api/$aktorIdMedForskuttering/forskuttering?orgnummer=333" -> {
                    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(ByteReadChannel("{\"forskuttering\":\"JA\"}".toByteArray(Charsets.UTF_8)), HttpStatusCode.OK, responseHeaders)
                }
                    "https://tjenester.nav.no/api/$aktorIdUtenForskuttering/forskuttering?orgnummer=333" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(ByteReadChannel("{\"forskuttering\":\"NEI\"}".toByteArray(Charsets.UTF_8)), HttpStatusCode.OK, responseHeaders)
                }
                "https://tjenester.nav.no/api/$aktorIdMedUkjentForskuttering/forskuttering?orgnummer=333" -> {
                    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(ByteReadChannel("{\"forskuttering\":\"UKJENT\"}".toByteArray(Charsets.UTF_8)), HttpStatusCode.OK, responseHeaders)
                }
                "https://login.microsoftonline.com/token" -> {
                    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    val expiresOn: String = Instant.now().plusSeconds(120L).toString()
                    respond(ByteReadChannel("{\"access_token\":\"xyz1234\",\"expires_on\":\"$expiresOn\"}".toByteArray(Charsets.UTF_8)), HttpStatusCode.OK, responseHeaders)
                }
                else -> error("Unhandled ${request.url.fullUrl}")
            }
        }
    }
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

@InternalAPI
val accessTokenClient = AccessTokenClient("https://login.microsoftonline.com/token", "clientid", "clientsecret", client)

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

private fun TestApplicationEngine.initTestAuthentication() {
    application.authentication {
        jwt {
            skipWhen { call ->
                call.request.uri.contains("arbeidsgiverForskutterer")
            }
        }
    }
}
