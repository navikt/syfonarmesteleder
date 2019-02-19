package no.nav.syfo.narmesteLederApi

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.syfo.narmesteLederApi.Tilgang.*
import no.nav.syfo.traceinterceptor.withTraceInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Route.registrerNarmesteLederApi(narmesteLederClient: NarmesteLederClient) {
    get("/syfonarmesteleder/{arbeidsgiverAktorId}/tilganger") {
        withTraceInterceptor {
            try {
                val arbeidsgiverAktorId: String = call.parameters["arbeidsgiverAktorId"]?.takeIf { it.isNotEmpty() }
                        ?: throw IllegalArgumentException("ArbeidsgiverAktorId mangler")

                log.info("Mottatt forespørsel om nærmeste leder-barn for aktør {}", arbeidsgiverAktorId)

                val narmesteLeder = narmesteLederClient.hentNarmesteLederFraSyfoserviceStrangler(arbeidsgiverAktorId)
                        .map { narmesteLederRelasjon ->
                            NarmesteLederTilgang(
                                    aktor = narmesteLederRelasjon.aktorId,
                                    orgnummer = narmesteLederRelasjon.orgnummer,
                                    tilganger = listOf(SYKMELDING, SYKEPENGESOKNAD, MOTE, OPPFOLGINGSPLAN))
                        }

                call.respond(narmesteLeder)

            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente forskuttering: {}", e.message)
                call.respond(HttpStatusCode.BadRequest, e.message!!)
            }
        }
    }
}

data class NarmesteLederTilgang(
        val aktor: String,
        val orgnummer: String,
        val tilganger: List<Tilgang>
)

enum class Tilgang {
    SYKMELDING,
    SYKEPENGESOKNAD,
    MOTE,
    OPPFOLGINGSPLAN
}
