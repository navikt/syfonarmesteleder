package no.nav.syfo.narmestelederapi

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.coroutines.withContext
import no.nav.syfo.CoroutineMDCContext
import no.nav.syfo.traceinterceptor.withTraceInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Route.registrerNarmesteLederApi(narmesteLederClient: NarmesteLederClient) {
    get("/syfonarmesteleder/narmesteLeder/{narmesteLederAktorId}") {
        withContext(CoroutineMDCContext()) {
            withTraceInterceptor {
                try {
                    val narmesteLederAktorId: String = call.parameters["narmesteLederAktorId"]?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException("NarmesteLederAktorId mangler")

                    log.info("Mottatt forespørsel om nærmeste leder-relasjoner for leder {}", narmesteLederAktorId)

                    call.respond(narmesteLederClient.hentNarmesteLederFraSyfoserviceStrangler(narmesteLederAktorId))

                } catch (e: IllegalArgumentException) {
                    log.warn("Kan ikke hente nærmeste leder: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente nærmeste leder")
                }
            }
        }
    }

    get("/syfonarmesteleder/sykmeldt/{sykmeldtAktorId}") {
        withContext(CoroutineMDCContext()) {
            withTraceInterceptor {
                try {
                    val sykmeldtAktorId: String = call.parameters["sykmeldtAktorId"]?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException("sykmeldtAktorId mangler")
                    val orgnummer: String = call.request.queryParameters["orgnummer"]?.takeIf { it.isNotEmpty() }
                            ?: throw NotImplementedError("Spørring uten orgnummer er ikke implementert")

                    val narmesteLederRelasjon = narmesteLederClient.hentNarmesteLederForSykmeldtFraSyfoserviceStrangler(sykmeldtAktorId, orgnummer)
                    call.respond(mapOf("narmesteLederRelasjon" to narmesteLederRelasjon))
                } catch (e: IllegalArgumentException) {
                    log.warn("Kan ikke hente nærmeste leder da aktørid mangler: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, e.message!!)
                } catch (e: NotImplementedError) {
                    log.info("Spørring uten orgnummer er ikke implementert", e.message)
                    call.respond(HttpStatusCode.BadRequest, e.message!!)
                }
            }
        }
    }
}
