package no.nav.syfo.narmesteleder

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.pdl.model.Navn
import no.nav.syfo.pdl.service.PdlPersonService

@KtorExperimentalAPI
class UtvidetNarmesteLederService(private val narmesteLederClient: NarmesteLederClient, private val pdlPersonService: PdlPersonService) {
    suspend fun hentNarmesteledereMedNavn(sykmeldtAktorId: String, callId: String): List<NarmesteLederRelasjon> {
        val narmesteLederRelasjoner = narmesteLederClient.hentNarmesteLedereForSykmeldtFraSyfoserviceStrangler(sykmeldtAktorId)
        val nlAktorIds = narmesteLederRelasjoner.map { it.narmesteLederAktorId }
        val nlNavn = pdlPersonService.getPersonnavn(aktorIds = nlAktorIds, callId = callId)

        return narmesteLederRelasjoner.map { it.copy(navn = nlNavn[it.narmesteLederAktorId]?.tilString()) }
    }

    private fun Navn.tilString(): String {
        return if (mellomnavn.isNullOrEmpty()) {
            "$fornavn $etternavn"
        } else {
            "$fornavn $mellomnavn $etternavn"
        }
    }
}
