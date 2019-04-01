package no.nav.syfo

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccessTokenClient(
        private val aadAccessTokenUrl: String,
        private val clientId: String,
        private val clientSecret: String,
        private val client: HttpClient
) {
    private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

    suspend fun hentAccessToken(resource: String): String {
        log.trace("Henter token fra Azure AD")
        // TODO: Remove this workaround whenever ktor issue #1009 is fixed
        val response: AadAccessToken = client.post<HttpResponse>(aadAccessTokenUrl) {
            accept(ContentType.Application.Json)
            method = HttpMethod.Post
            body = FormDataContent(Parameters.build {
                append("client_id", clientId)
                append("resource", resource)
                append("grant_type", "client_credentials")
                append("client_secret", clientSecret)
            })
        }.use { it.call.response.receive<AadAccessToken>() }
        log.trace("Har hentet accesstoken")
        return response.access_token
    }
}

private data class AadAccessToken(
        val access_token: String,
        val token_type: String,
        val expires_in: String,
        val ext_expires_in: String,
        val expires_on: String,
        val not_before: String,
        val resource: String
)
