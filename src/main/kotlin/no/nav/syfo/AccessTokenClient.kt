package no.nav.syfo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class AccessTokenClient(
        private val aadAccessTokenUrl: String,
        private val clientId: String,
        private val clientSecret: String,
        private val client: HttpClient
) {
    private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

    suspend fun hentAccessToken(resource: String): String {
        log.trace("Henter token fra Azure AD")
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
        log.trace("Har hentet accesstoken")
        System.out.println("Token med timestamp: " + response.expires_on)
        return response.access_token
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class AadAccessToken(
        val access_token: String,
        val expires_on: Instant
)
