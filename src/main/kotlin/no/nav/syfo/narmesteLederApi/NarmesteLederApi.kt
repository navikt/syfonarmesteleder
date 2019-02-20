package no.nav.syfo.narmesteLederApi

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.syfo.traceinterceptor.withTraceInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Route.registrerNarmesteLederApi(narmesteLederClient: NarmesteLederClient) {
    get("/syfonarmesteleder/narmesteLeder/{narmesteLederAktorId}") {
        withTraceInterceptor {
            try {
                val narmesteLederAktorId: String = call.parameters["narmesteLederAktorId"]?.takeIf { it.isNotEmpty() }
                        ?: throw IllegalArgumentException("NarmesteLederAktorId mangler")

                log.info("Mottatt forespørsel om nærmeste leder-relasjoner for leder {}", narmesteLederAktorId)

                val narmesteLeder = narmesteLederClient.hentNarmesteLederFraSyfoserviceStrangler(narmesteLederAktorId)

                call.respond(narmesteLeder)

            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente nærmeste leder: {}", e.message)
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente nærmeste leder")
            }
        }
    }
}
