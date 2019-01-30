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
import java.lang.IllegalArgumentException

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

@KtorExperimentalAPI
fun Routing.registrerForskutteringApi(forskutteringsClient: ForskutteringsClient) {
    get("/syfonarmesteleder/arbeidsgiverForskutterer") {
        val request = call.request
        try {
            MDC.put("Nav-Callid", request.header("Nav-Callid"))
            MDC.put("Nav-Consumer-Id", request.header("Nav-Consumer-Id"))

            val queryParameters: Parameters = request.queryParameters
            val aktoerid: String = queryParameters["aktoerid"]?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Aktoerid mangler")
            val orgnr: String = queryParameters["orgnr"]?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Orgnr mangler")

            log.info("Mottatt forespørsel om forskuttering for aktør {} og orgnr {}", aktoerid, orgnr)

            val arbeidsgiverForskutterer = forskutteringsClient.hentNarmesteLederFraSyfoserviceStrangler(aktoerid, orgnr, request.authorization())
            call.respond(arbeidsgiverForskutterer)

        } catch (e: IllegalArgumentException) {
            log.warn("Kan ikke hente forskuttering: {}", e.message)
            call.respond(HttpStatusCode.BadRequest, e.message!!)
        } finally {
            MDC.remove("Nav-Callid")
            MDC.remove("Nav-Consumer-Id")
        }
    }
}
