package no.nav.syfo.syfoservice

import java.time.LocalDateTime

data class NarmesteLederDTO(
    val narmesteLederId: Long,
    val brukerFnr: String,
    val orgnummer: String,
    val nlfnr: String,
    val nlTelefonnummer: String,
    val nlEpost: String,
    val aktivFom: LocalDateTime,
    val aktivTom: LocalDateTime?,
    val agForskutterer: Boolean?
)

fun NarmesteLederDTO.toForskuttering(): Forskuttering =
    Forskuttering(
        brukerFnr = this.brukerFnr,
        orgnummer = this.orgnummer,
        arbeidsgiverForskutterer = this.agForskutterer,
        sistOppdatert = LocalDateTime.now()
    )

fun NarmesteLederDTO.toNarmesteLeder(): NarmesteLeder =
    NarmesteLeder(
        narmesteLederId = this.narmesteLederId.toString(),
        orgnummer = this.orgnummer,
        brukerFnr = this.brukerFnr,
        narmesteLederFnr = this.nlfnr,
        narmesteLederTelefonnummer = this.nlTelefonnummer,
        narmesteLederEpost = this.nlEpost,
        aktivFom = this.aktivFom,
        aktivTom = this.aktivTom
    )
