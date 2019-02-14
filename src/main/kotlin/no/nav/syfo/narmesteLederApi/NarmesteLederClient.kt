package no.nav.syfo.narmesteLederApi

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import no.nav.syfo.AccessTokenClient
import org.slf4j.MDC
import java.time.LocalDate

class NarmesteLederClient(
        private val endpointUrl: String,
        private val resourceId: String,
        private val accessTokenClient: AccessTokenClient,
        private val client: HttpClient
) {
    suspend fun hentNarmesteLederFraSyfoserviceStrangler(hrAktorId: String): List<NarmesteLeder> {
        val accessToken = accessTokenClient.hentAccessToken(resourceId)
        return client.get("$endpointUrl/api/$hrAktorId/narmesteleder") {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $accessToken")
                append("Nav-Consumer-Id", MDC.get("Nav-Consumer-Id"))
                append("Nav-Callid", MDC.get("Nav-Callid"))
            }
        }
    }
}

data class NarmesteLeder(
        val aktorId: String,
        val orgnummer: String,
        val nlAktorId: String,
        val nlTelefonnummer: String?,
        val nlEpost: String?,
        val aktivFom: LocalDate,
        val agForskutterer: Boolean?
)
