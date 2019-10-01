package no.nav.syfo.syfoservice

import java.time.LocalDateTime

data class NarmesteLeder(
    val narmesteLederId: String,
    val orgnummer: String,
    val brukerFnr: String,
    val narmesteLederFnr: String,
    val narmesteLederTelefonnummer: String,
    val narmesteLederEpost: String,
    val aktivFom: LocalDateTime,
    val aktivTom: LocalDateTime?
)
