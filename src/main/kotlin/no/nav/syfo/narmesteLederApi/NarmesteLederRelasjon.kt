package no.nav.syfo.narmesteLederApi

import java.time.LocalDate

data class NarmesteLederRelasjon(
        val aktorId: String,
        val orgnummer: String,
        val narmesteLederAktorId: String,
        val narmesteLederTelefonnummer: String?,
        val narmesteLederEpost: String?,
        val aktivFom: LocalDate,
        val narmesteLederForskutterer: Boolean?,
        val skrivetilgang: Boolean,
        val tilganger: List<Tilgang>
)

enum class Tilgang {
    SYKMELDING,
    SYKEPENGESOKNAD,
    MOTE,
    OPPFOLGINGSPLAN
}
