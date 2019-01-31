package no.nav.syfo.forskuttering

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import org.slf4j.MDC

class ForskutteringsClient(private val endpointUrl: String, private val client: HttpClient) {
    suspend fun hentNarmesteLederFraSyfoserviceStrangler(aktorId: String, orgnummer: String, authorization: String?): ForskutteringRespons =
            client.get("$endpointUrl/api/$aktorId/forskuttering") {
                accept(ContentType.Application.Json)
                headers {
                    append("Authorization", authorization.orEmpty())
                    append("Nav-Consumer-Id", MDC.get("Nav-Consumer-Id"))
                    append("Nav-Callid", MDC.get("Nav-Callid"))
                }
                parameter("orgnummer", orgnummer)
            }
}

class ForskutteringRespons(private val forskuttering: Forskuttering)

enum class Forskuttering {
    JA, NEI, UKJENT
}
