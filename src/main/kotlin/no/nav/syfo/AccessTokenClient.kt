package no.nav.syfo

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccessTokenClient(private val aadAccessTokenUrl: String, private val clientId: String, private val clientSecret: String, private val client: HttpClient) {
    private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

    suspend fun hentAccessToken(resource: String): String {
        log.info("Henter token")
        val response: AadAccessToken = client.post(aadAccessTokenUrl) {
            accept(ContentType.Application.Json)
            parameter("client_id", clientId)
            parameter("resource", resource)
            parameter("grant_type", "client_credentials")
            parameter("client_secret", clientSecret)
        }
        //FJERNES!!!
        log.info("Accesstoken: {}", response.access_token)
        return response.access_token
    }
}

private class AadAccessToken(
        val access_token: String,
        val token_type: String,
        val expires_in: String,
        val expires_on: String,
        val resource: String
)
