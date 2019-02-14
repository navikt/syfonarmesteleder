package no.nav.syfo.forskuttering

import io.ktor.application.install
import io.ktor.auth.authentication
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockHttpResponse
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.*
import io.ktor.request.uri
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import kotlinx.coroutines.io.ByteReadChannel
import no.nav.syfo.AccessTokenClient
import no.nav.syfo.ApplicationState
import no.nav.syfo.getEnvironment
import no.nav.syfo.initRouting
import no.nav.syfo.narmesteLederApi.NarmesteLederClient
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldMatch
import org.amshove.kluent.shouldNotEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

const val aktorIdMedForskuttering = 123
const val aktorIdUtenForskuttering = 999
const val aktorIdMedUkjentForskuttering = 678

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
                gson {
                    setPrettyPrinting()
                }
            }
            it("Returnerer JA hvis arbeidsgiver forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktorId=$aktorIdMedForskuttering&orgnummer=333")) {
                    response.content?.shouldMatch( "\\{\\n\\s{2}\"forskuttering\":\\s\"JA\"\\n}")
                }
            }
            it("Returnerer NEI hvis arbeidsgiver ikke forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktorId=$aktorIdUtenForskuttering&orgnummer=333")) {
                    response.content?.shouldMatch( "\\{\\n\\s{2}\"forskuttering\":\\s\"NEI\"\\n}")
                }
            }
            it("Returnerer UKJENT hvis vi ikke vet om arbeidsgiver forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktorId=$aktorIdMedUkjentForskuttering&orgnummer=333")) {
                    response.content?.shouldMatch( "\\{\\n\\s{2}\"forskuttering\":\\s\"UKJENT\"\\n}")
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
                gson {
                    setPrettyPrinting()
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

val httpMockEngine: HttpClientEngine = MockEngine {
    when (this.url.fullUrl) {
        "https://tjenester.nav.no/api/$aktorIdMedForskuttering/forskuttering?orgnummer=333" -> {
            MockHttpResponse(
                    call,
                    HttpStatusCode.OK,
                    ByteReadChannel("{\"forskuttering\":\"JA\"}".toByteArray(Charsets.UTF_8)),
                    headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            )
        }
        "https://tjenester.nav.no/api/$aktorIdUtenForskuttering/forskuttering?orgnummer=333" -> {
            MockHttpResponse(
                    call,
                    HttpStatusCode.OK,
                    ByteReadChannel("{\"forskuttering\":\"NEI\"}".toByteArray(Charsets.UTF_8)),
                    headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            )
        }
        "https://tjenester.nav.no/api/$aktorIdMedUkjentForskuttering/forskuttering?orgnummer=333" -> {
            MockHttpResponse(
                    call,
                    HttpStatusCode.OK,
                    ByteReadChannel("{\"forskuttering\":\"UKJENT\"}".toByteArray(Charsets.UTF_8)),
                    headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            )
        }
        "https://login.microsoftonline.com/token" -> {
            MockHttpResponse(
                    call,
                    HttpStatusCode.OK,
                    ByteReadChannel("{\"access_token\":\"xyz1234\"}".toByteArray(Charsets.UTF_8)),
                    headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            )
        }
        else -> {
            error("Unhandled ${url.fullUrl}")
        }
    }
}

val client = HttpClient(httpMockEngine) {
    install(JsonFeature) {
        serializer = GsonSerializer()
    }
}

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
