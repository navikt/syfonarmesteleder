package no.nav.syfo

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
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
        log.info("Henter token")
        val response: AadAccessToken = client.post(aadAccessTokenUrl) {
            accept(ContentType.Application.Json)
            method = HttpMethod.Post
            body = FormDataContent(Parameters.build {
                append("client_id", clientId)
                append("resource", resource)
                append("grant_type", "client_credentials")
                append("client_secret", clientSecret)
            })
        }
        log.info("Har hentet accesstoken")
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
