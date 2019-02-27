package no.nav.syfo.narmestelederapi

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import no.nav.syfo.AccessTokenClient
import no.nav.syfo.missingCallId
import no.nav.syfo.missingConsumerId
import org.slf4j.MDC
import java.time.LocalDate

class NarmesteLederClient(
        private val endpointUrl: String,
        private val resourceId: String,
        private val accessTokenClient: AccessTokenClient,
        private val client: HttpClient
) {
    suspend fun hentNarmesteLederFraSyfoserviceStrangler(nlAktorId: String): List<NarmesteLederRelasjon> {
        val accessToken = accessTokenClient.hentAccessToken(resourceId)
        return client.get<List<NarmesteLeder>>("$endpointUrl/arbeidsgiver/$nlAktorId/narmesteleder") {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $accessToken")
                append("Nav-Consumer-Id", MDC.get("Nav-Consumer-Id") ?: missingConsumerId("hentNarmesteLederFraSyfoserviceStrangler"))
                append("Nav-Callid", MDC.get("Nav-Callid") ?: missingCallId("hentNarmesteLederFraSyfoserviceStrangler"))
            }
        }.map {
            NarmesteLederRelasjon(
                    aktorId = it.aktorId,
                    orgnummer = it.orgnummer,
                    narmesteLederAktorId = it.nlAktorId,
                    narmesteLederTelefonnummer = it.nlTelefonnummer,
                    narmesteLederEpost = it.nlEpost,
                    aktivFom = it.aktivFom,
                    arbeidsgiverForskutterer = it.agForskutterer,
                    skrivetilgang = true,
                    tilganger = listOf(Tilgang.SYKMELDING, Tilgang.SYKEPENGESOKNAD, Tilgang.MOTE, Tilgang.OPPFOLGINGSPLAN))
        }
    }

    suspend fun hentNarmesteLederForSykmeldtFraSyfoserviceStrangler(sykmeldtAktorId: String, orgnummer: String): NarmesteLederRelasjon? {
        val accessToken = accessTokenClient.hentAccessToken(resourceId)
        return client.get<NarmesteLederResponse>("$endpointUrl/sykmeldt/$sykmeldtAktorId/narmesteleder?orgnummer=$orgnummer") {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $accessToken")
                append("Nav-Consumer-Id", MDC.get("Nav-Consumer-Id") ?: missingConsumerId("hentNarmesteLederForSykmeldtFraSyfoserviceStrangler"))
                append("Nav-Callid", MDC.get("Nav-Callid") ?: missingCallId("hentNarmesteLederForSykmeldtFraSyfoserviceStrangler"))
            }
        }.let {
            it.narmesteleder
        }?.let {
            NarmesteLederRelasjon(
                    aktorId = it.aktorId,
                    orgnummer = it.orgnummer,
                    narmesteLederAktorId = it.nlAktorId,
                    narmesteLederTelefonnummer = it.nlTelefonnummer,
                    narmesteLederEpost = it.nlEpost,
                    aktivFom = it.aktivFom,
                    arbeidsgiverForskutterer = it.agForskutterer,
                    skrivetilgang = true,
                    tilganger = listOf(Tilgang.SYKMELDING, Tilgang.SYKEPENGESOKNAD, Tilgang.MOTE, Tilgang.OPPFOLGINGSPLAN))
        }
    }
}

data class NarmesteLederResponse(val narmesteleder: NarmesteLeder?)

data class NarmesteLeder(
        val aktorId: String,
        val orgnummer: String,
        val nlAktorId: String,
        val nlTelefonnummer: String?,
        val nlEpost: String?,
        val aktivFom: LocalDate,
        val agForskutterer: Boolean?
)
