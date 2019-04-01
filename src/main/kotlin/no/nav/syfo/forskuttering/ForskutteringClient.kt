package no.nav.syfo.forskuttering

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import no.nav.syfo.AccessTokenClient
import org.slf4j.MDC

class ForskutteringsClient(
        private val endpointUrl: String,
        private val resourceId: String,
        private val accessTokenClient: AccessTokenClient,
        private val client: HttpClient
) {
    suspend fun hentForskutteringFraSyfoserviceStrangler(aktorId: String, orgnummer: String): ForskutteringRespons {
        val accessToken = accessTokenClient.hentAccessToken(resourceId)
        // TODO: Remove this workaround whenever ktor issue #1009 is fixed
        return client.get<HttpResponse>("$endpointUrl/api/$aktorId/forskuttering") {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $accessToken")
                append("Nav-Consumer-Id", MDC.get("Nav-Consumer-Id"))
                append("Nav-Callid", MDC.get("Nav-Callid"))
            }
            parameter("orgnummer", orgnummer)
        }.use { it.call.response.receive<ForskutteringRespons>() }
    }
}

class ForskutteringRespons(private val forskuttering: Forskuttering)

enum class Forskuttering {
    JA, NEI, UKJENT
}
