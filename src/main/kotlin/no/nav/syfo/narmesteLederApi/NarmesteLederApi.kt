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
    get("/syfonarmesteleder/{hrAktorId}/hrtilganger") {
        withTraceInterceptor {
            try {
                val hrAktorId: String = call.parameters["hrAktorId"]?.takeIf { it.isNotEmpty() }
                        ?: throw IllegalArgumentException("HrAktorId mangler")

                log.info("Mottatt forespørsel om nærmeste leder-barn for aktør {}", hrAktorId)

                val narmesteLeder = narmesteLederClient.hentNarmesteLederFraSyfoserviceStrangler(hrAktorId)
                        .map { narmesteLeder ->
                            Entitet(
                                    aktor = narmesteLeder.aktorId,
                                    orgnummer = narmesteLeder.orgnummer,
                                    tilganger = listOf(SYKMELDING, SYKEPENGESOKNAD, MOTE, OPPFOLGINGSPLAN))
                        }

                call.respond(TilgangRespons(narmesteLeder, emptyList()))

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
        val tilganger: List<Tilgang>
)

enum class Tilgang {
    SYKMELDING,
    SYKEPENGESOKNAD,
    MOTE,
    OPPFOLGINGSPLAN
}
