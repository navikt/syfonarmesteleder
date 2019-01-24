package no.nav.syfo.forskuttering

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType

class ForskutteringsClient(private val endpointUrl: String) {

    private val client = HttpClient(Apache)

    fun hentNarmesteLeder(forskutteringsRequest: ForskutteringRequest): Forskuttering? = Forskuttering.JA

    private suspend fun hentNarmesteLederFraSyfoserviceStrangler(forskutteringsRequest: ForskutteringRequest): String =
            client.get("$endpointUrl/hentNarmesteleder") {
                accept(ContentType.Application.Json)
                headers {
                    append("Authorization", forskutteringsRequest.authorization.orEmpty())
                    append("Nav-Consumer-Id", forskutteringsRequest.consumerId.orEmpty())
                    append("Nav-Call-Id", forskutteringsRequest.callId.orEmpty())
                }
                parameter("aktoerid", forskutteringsRequest.aktoerid)
                parameter("orgnr", forskutteringsRequest.orgnr)
            }

    enum class Forskuttering {
        JA, NEI, VETIKKE
    }
}
