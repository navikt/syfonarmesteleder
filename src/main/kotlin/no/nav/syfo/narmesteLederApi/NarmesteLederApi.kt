package no.nav.syfo.narmesteLederApi

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.syfo.traceinterceptor.withTraceInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Route.registrerNarmesteLederApi(narmesteLederClient: NarmesteLederClient) {
    get("/syfonarmesteleder/hrtilganger/{hrAktorId}/{orgnummer}") {
        withTraceInterceptor {
            try {
                val hrAktorId: String = call.parameters["hrAktorId"]?.takeIf { it.isNotEmpty() }
                        ?: throw IllegalArgumentException("HrAktorId mangler")
                val orgnummer: String = call.parameters["orgnummer"]?.takeIf { it.isNotEmpty() }
                        ?: throw IllegalArgumentException("Orgnummer mangler")

                log.info("Mottatt forespørsel om nærmeste leder-barn for aktør {} og orgnummer {}", hrAktorId, orgnummer)

                val ledere = narmesteLederClient.hentNarmesteLederFraSyfoserviceStrangler(hrAktorId, orgnummer)

                call.respond(ledere)

            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente forskuttering: {}", e.message)
                call.respond(HttpStatusCode.BadRequest, e.message!!)
            }
        }
    }
}

data class TilgangRespons(
        val narmesteLeder: List<Entitet> = emptyList(),
        val humanResources: List<Entitet> = emptyList()
)

data class Entitet(
        val aktor: String? = null,
        val orgnummer: String,
        val skrivetilgang: Boolean,
        val tilganger: List<Tilgang>
)

enum class Tilgang {
    SYKMELDING,
    SYKEPENGESOKNAD,
    MOTE,
    OPPFOLGINGSPLAN
}
