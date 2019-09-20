package no.nav.syfo.syfoservice

import java.time.LocalDateTime

data class ForskutteringDAO(
    val brukerFnr: String,
    val orgnummer: String,
    val arbeidsgiverForskutterer: Boolean?,
    val sistOppdatert: LocalDateTime
)
