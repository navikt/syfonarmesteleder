package no.nav.syfo.forskuttering

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.coroutines.withContext
import no.nav.syfo.CoroutineMDCContext
import no.nav.syfo.traceinterceptor.withTraceInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Route.registrerForskutteringApi(forskutteringsClient: ForskutteringsClient) {
    get("/syfonarmesteleder/arbeidsgiverForskutterer") {
        withContext(CoroutineMDCContext()) {
            withTraceInterceptor {
                val request = call.request
                try {
                    val queryParameters: Parameters = request.queryParameters
                    val aktorId: String = queryParameters["aktorId"]?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException("AktorId mangler")
                    val orgnummer: String = queryParameters["orgnummer"]?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException("Orgnummer mangler")

                    log.info("Mottatt forespørsel om forskuttering for aktør {} og orgnummer {}", aktorId, orgnummer)

                    val arbeidsgiverForskutterer = forskutteringsClient.hentForskutteringFraSyfoserviceStrangler(aktorId, orgnummer)
                    call.respond(arbeidsgiverForskutterer)

                } catch (e: IllegalArgumentException) {
                    log.warn("Kan ikke hente forskuttering: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, e.message!!)
                }
            }
        }
    }
}
