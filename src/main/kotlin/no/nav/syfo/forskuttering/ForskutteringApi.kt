package no.nav.syfo.forskuttering

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.authorization
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

@KtorExperimentalAPI
fun Routing.registrerForskutteringApi(forskutteringsClient: ForskutteringsClient) {
    get("/syfonarmesteleder/arbeidsgiverForskutterer") {
        val request = call.request
        try {
            MDC.put("Nav-Callid", request.header("Nav-Callid"))
            MDC.put("Nav-Consumer-Id", request.header("Nav-Consumer-Id"))

            val queryParameters: Parameters = request.queryParameters
            val aktoerid: String? = queryParameters["aktoerid"]
            val orgnr: String? = queryParameters["orgnr"]

            log.info("Mottatt forespørsel om forskuttering for aktør {} og orgnr {}", aktoerid, orgnr)

            if (aktoerid?.isNotEmpty() == true && orgnr?.isNotEmpty() == true) {
                val arbeidsgiverForskutterer = forskutteringsClient.hentNarmesteLederFraSyfoserviceStrangler(aktoerid, orgnr, request.authorization())
                call.respond(arbeidsgiverForskutterer)
            } else {
                call.respond(HttpStatusCode.BadRequest, "Aktoerid og/eller orgnr mangler")
            }
        } finally {
            MDC.remove("Nav-Callid")
            MDC.remove("Nav-Consumer-Id")
        }
    }
}
