package no.nav.syfo.narmesteleder

import io.ktor.util.KtorExperimentalAPI
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import no.nav.syfo.pdl.model.Navn
import no.nav.syfo.pdl.service.PdlPersonService
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

@KtorExperimentalAPI
class UtvidetNarmesteLederServiceTest : Spek({
    val narmesteLederClient = mockkClass(NarmesteLederClient::class)
    val pdlPersonService = mockkClass(PdlPersonService::class)
    val utvidetNarmesteLederService = UtvidetNarmesteLederService(narmesteLederClient, pdlPersonService)

    val callId = "callid"
    val sykmeldtAktorId = "aktorId"
    val aktorIdLeder1 = "123"
    val aktorIdLeder2 = "456"

    beforeEachTest {
        clearMocks(narmesteLederClient)
        clearMocks(pdlPersonService)
        coEvery { narmesteLederClient.hentNarmesteLedereForSykmeldtFraSyfoserviceStrangler(any()) } returns listOf(
            NarmesteLederRelasjon(
                aktorId = sykmeldtAktorId,
                orgnummer = "orgnummer",
                narmesteLederAktorId = aktorIdLeder1,
                narmesteLederTelefonnummer = null,
                narmesteLederEpost = "epost@nav.no",
                aktivFom = LocalDate.now().minusYears(1),
                aktivTom = null,
                arbeidsgiverForskutterer = true,
                skrivetilgang = true,
                tilganger = emptyList()
            ),
            NarmesteLederRelasjon(
                aktorId = sykmeldtAktorId,
                orgnummer = "orgnummer2",
                narmesteLederAktorId = aktorIdLeder2,
                narmesteLederTelefonnummer = null,
                narmesteLederEpost = "epost2@nav.no",
                aktivFom = LocalDate.now().minusYears(2),
                aktivTom = LocalDate.now().minusYears(1),
                arbeidsgiverForskutterer = true,
                skrivetilgang = false,
                tilganger = emptyList()
            )
        )
    }

    describe("UtvidetNarmesteLederService") {
        it("Setter riktig navn på ledere") {
            coEvery { pdlPersonService.getPersonnavn(any(), any()) } returns mapOf(
                Pair(aktorIdLeder1, Navn("Fornavn", null, "Etternavn")),
                Pair(aktorIdLeder2, Navn("Fornavn2", "Mellomnavn", "Etternavn2"))
            )

            runBlocking {
                val narmesteLedereMedNavn = utvidetNarmesteLederService.hentNarmesteledereMedNavn(sykmeldtAktorId, callId)

                narmesteLedereMedNavn.size shouldBeEqualTo 2
                val nl1 = narmesteLedereMedNavn.find { it.narmesteLederAktorId == aktorIdLeder1 }
                nl1?.navn shouldBeEqualTo "Fornavn Etternavn"
                val nl2 = narmesteLedereMedNavn.find { it.narmesteLederAktorId == aktorIdLeder2 }
                nl2?.navn shouldBeEqualTo "Fornavn2 Mellomnavn Etternavn2"
            }
        }
        it("Setter null som navn hvis navn mangler i PDL (feiler ikke)") {
            coEvery { pdlPersonService.getPersonnavn(any(), any()) } returns mapOf(
                Pair(aktorIdLeder1, Navn("Fornavn", null, "Etternavn")),
                Pair(aktorIdLeder2, null)
            )

            runBlocking {
                val narmesteLedereMedNavn = utvidetNarmesteLederService.hentNarmesteledereMedNavn(sykmeldtAktorId, callId)

                narmesteLedereMedNavn.size shouldBeEqualTo 2
                val nl1 = narmesteLedereMedNavn.find { it.narmesteLederAktorId == aktorIdLeder1 }
                nl1?.navn shouldBeEqualTo "Fornavn Etternavn"
                val nl2 = narmesteLedereMedNavn.find { it.narmesteLederAktorId == aktorIdLeder2 }
                nl2?.navn shouldBeEqualTo null
            }
        }
    }
})
