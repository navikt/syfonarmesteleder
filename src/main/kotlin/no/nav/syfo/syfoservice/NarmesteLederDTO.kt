package no.nav.syfo.syfoservice

import java.time.LocalDateTime

data class NarmesteLederDTO(
    val narmesteLederId: Long,
    val bruerkFnr: String,
    val orgnummer: String,
    val nlfnr: String,
    val nlTelefonnummer: String,
    val nlEpost: String,
    val aktivFom: LocalDateTime,
    val aktivTom: LocalDateTime?,
    val agForskutterer: Boolean?
)

fun NarmesteLederDTO.toForskutteringDAO(): ForskutteringDAO =
    ForskutteringDAO(
        brukerFnr = this.bruerkFnr,
        orgnummer = this.orgnummer,
        arbeidsgiverForskutterer = this.agForskutterer,
        sistOppdatert = LocalDateTime.now()
    )

fun NarmesteLederDTO.toNarmesteLederDAO(): NarmesteLederDAO =
    NarmesteLederDAO(
        narmesteLederId = this.narmesteLederId.toString(),
        orgnummer = this.orgnummer,
        brukerFnr = this.bruerkFnr,
        narmesteLederFnr = this.nlfnr,
        narmesteLederTelefonnummer = this.nlTelefonnummer,
        narmesteLederEpost = this.nlEpost,
        aktivFom = this.aktivFom,
        aktivTom = this.aktivTom
    )