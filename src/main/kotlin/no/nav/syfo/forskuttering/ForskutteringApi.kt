package no.nav.syfo.forskuttering

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.ApplicationRequest
import io.ktor.request.authorization
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Routing.registrerForskutteringApi(forskutteringsClient: ForskutteringsClient) {
    get("/syfonarmesteleder/arbeidsgiverForskutterer") {
        val request = call.request
        val queryParameters: Parameters = request.queryParameters
        val aktoerid: String? = queryParameters["aktoerid"]
        val orgnr: String? = queryParameters["orgnr"]
        log.info("Mottatt forespørsel om forskuttering for aktør {} og orgnr {}", aktoerid, orgnr)

        if (aktoerid != null && aktoerid.isNotEmpty() && orgnr != null && orgnr.isNotEmpty()) {
            val arbeidsgiverForskutterer = forskutteringsClient.hentNarmesteLeder(forskutteringsRequestFraHttpRequest(request, aktoerid, orgnr))
            if (arbeidsgiverForskutterer != null) {
                call.respondText(arbeidsgiverForskutterer.name)
            } else {
                call.respondText("null")
            }
        } else {
            call.respond(HttpStatusCode.BadRequest, "Aktoerid og/eller orgnr mangler")
        }
    }
}

fun forskutteringsRequestFraHttpRequest(request: ApplicationRequest, aktoerid: String, orgnr: String): ForskutteringRequest =
        ForskutteringRequest(
                request.header("Nav-Consumer-Id"),
                request.header("Nav-Call-Id"),
                request.authorization(),
                aktoerid,
                orgnr)

data class ForskutteringRequest(
        val consumerId: String?,
        val callId: String?,
        val authorization: String?,
        val aktoerid: String,
        val orgnr: String
)
