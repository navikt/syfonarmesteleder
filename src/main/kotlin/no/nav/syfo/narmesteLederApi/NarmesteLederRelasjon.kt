package no.nav.syfo.narmesteLederApi

import java.time.LocalDate

data class NarmesteLederRelasjon(
        val aktorId: String,
        val orgnummer: String,
        val nlAktorId: String,
        val nlTelefonnummer: String?,
        val nlEpost: String?,
        val aktivFom: LocalDate,
        val agForskutterer: Boolean?
)
