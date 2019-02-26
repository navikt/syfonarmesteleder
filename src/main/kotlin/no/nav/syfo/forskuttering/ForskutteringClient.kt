package no.nav.syfo.forskuttering

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import no.nav.syfo.AccessTokenClient
import no.nav.syfo.missingCallId
import no.nav.syfo.missingConsumerId
import org.slf4j.MDC

class ForskutteringsClient(
        private val endpointUrl: String,
        private val resourceId: String,
        private val accessTokenClient: AccessTokenClient,
        private val client: HttpClient
) {
    suspend fun hentForskutteringFraSyfoserviceStrangler(aktorId: String, orgnummer: String): ForskutteringRespons {
        val accessToken = accessTokenClient.hentAccessToken(resourceId)
        return client.get("$endpointUrl/api/$aktorId/forskuttering") {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $accessToken")
                append("Nav-Consumer-Id", MDC.get("Nav-Consumer-Id") ?: missingConsumerId("hentForskutteringFraSyfoserviceStrangler"))
                append("Nav-Callid", MDC.get("Nav-Callid") ?: missingCallId("hentForskutteringFraSyfoserviceStrangler"))
            }
            parameter("orgnummer", orgnummer)
        }
    }
}

class ForskutteringRespons(private val forskuttering: Forskuttering)

enum class Forskuttering {
    JA, NEI, UKJENT
}
