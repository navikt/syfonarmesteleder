package no.nav.syfo.forskuttering

import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockHttpResponse
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.*
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.io.ByteReadChannel
import no.nav.syfo.ApplicationState
import no.nav.syfo.initRouting
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

const val aktoeridMedForskuttering = 123
const val aktoeridUtenForskuttering = 999

@KtorExperimentalAPI
object ForskutteringApiSpek : Spek({
    val applicationState = ApplicationState()
    val forskutteringsClient = ForskutteringsClient("https://tjenester.nav.no", client)

    describe("Forskutteringsapi returnerer gyldig svar for gyldig request") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registrerForskutteringApi(forskutteringsClient)
            }
            application.install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }
            it("Returnerer JA hvis arbeidsgiver forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktoerid=$aktoeridMedForskuttering&orgnr=333")) {
                    response.content?.shouldMatch( "\\{\\n\\s{2}\"forskuttering\":\\s\"JA\"\\n}")
                }
            }
            it("Returnerer NEI hvis arbeidsgiver ikke forskutterer") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktoerid=$aktoeridUtenForskuttering&orgnr=333")) {
                    response.content?.shouldMatch( "\\{\\n\\s{2}\"forskuttering\":\\s\"NEI\"\\n}")
                }
            }
        }
    }

    describe("Forskutteringsapi returnerer BadRequest for ugyldig request") {
        with(TestApplicationEngine()) {
            start()
            application.initRouting(applicationState)
            application.install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }

            it("Returnerer feilmelding hvis aktørid mangler") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?orgnr=333")) {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                    response.content shouldNotEqual null
                }
            }

            it("Returnerer feilmelding hvis orgnr mangler") {
                with(handleRequest(HttpMethod.Get, "/syfonarmesteleder/arbeidsgiverForskutterer?aktoerid=$aktoeridMedForskuttering")) {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                    response.content shouldNotEqual null
                }
            }
        }
    }
})

val httpMockEngine: HttpClientEngine = MockEngine {
    when (this.url.fullUrl) {
        "https://tjenester.nav.no/hentNarmesteleder?aktoerid=$aktoeridMedForskuttering&orgnr=333" -> {
            MockHttpResponse(
                    call,
                    HttpStatusCode.OK,
                    ByteReadChannel("{\"forskuttering\":\"JA\"}".toByteArray(Charsets.UTF_8)),
                    headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            )
        }
        "https://tjenester.nav.no/hentNarmesteleder?aktoerid=$aktoeridUtenForskuttering&orgnr=333" -> {
            MockHttpResponse(
                    call,
                    HttpStatusCode.OK,
                    ByteReadChannel("{\"forskuttering\":\"NEI\"}".toByteArray(Charsets.UTF_8)),
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

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"
