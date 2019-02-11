package no.nav.syfo.forskuttering

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Route.registrerForskutteringApi(forskutteringsClient: ForskutteringsClient) {
    get("/syfonarmesteleder/arbeidsgiverForskutterer") {
        val request = call.request
        try {
            MDC.put("Nav-Callid", request.header("Nav-Callid") ?: UUID.randomUUID().toString())
            MDC.put("Nav-Consumer-Id", request.header("Nav-Consumer-Id") ?: "syfonarmesteleder")

            val queryParameters: Parameters = request.queryParameters
            val aktorId: String = queryParameters["aktorId"]?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("AktorId mangler")
            val orgnummer: String = queryParameters["orgnummer"]?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Orgnummer mangler")

            log.info("Mottatt forespørsel om forskuttering for aktør {} og orgnummer {}", aktorId, orgnummer)

            val arbeidsgiverForskutterer = forskutteringsClient.hentNarmesteLederFraSyfoserviceStrangler(aktorId, orgnummer)
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
