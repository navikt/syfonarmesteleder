package no.nav.syfo.db

import kotliquery.*
import no.nav.syfo.getEnvVar
import no.nav.syfo.getEnvironment
import no.nav.syfo.narmestelederapi.NarmesteLeder
import no.nav.syfo.narmestelederapi.NarmesteLederRelasjon
import no.nav.syfo.narmestelederapi.Tilgang
import org.flywaydb.core.Flyway
import java.time.LocalDate
import java.time.ZonedDateTime

val toNarmesteLeder: (Row) -> NarmesteLeder = { row ->
    NarmesteLeder(
            row.string("aktorid"),
            row.string("orgnummer"),
            row.string("nlaktorid"),
            row.string("nltelefonnummer"),
            row.string("nlepost"),
            row.sqlDate("aktivfom").toLocalDate(),
            row.boolean("agforskutterer")
    )
}

val toNarmesteLederRelasjon: (Row) -> NarmesteLederRelasjon = { row ->
    NarmesteLederRelasjon(
            row.string("aktorid"),
            row.string("orgnummer"),
            row.string("nlaktorid"),
            row.string("nltelefonnummer"),
            row.string("nlepost"),
            row.sqlDate("aktivfom").toLocalDate(),
            row.int("agforskutterer").equals(1),
            true,
            listOf(Tilgang.SYKMELDING, Tilgang.SYKEPENGESOKNAD, Tilgang.MOTE, Tilgang.OPPFOLGINGSPLAN)
    )
}

class NarmesteLederDAO(val session: Session) {

    fun finnAktorLeder(aktorid: String, orgnummer: String): NarmesteLederRelasjon? {
        val selectQuery = queryOf("select * from narmesteleder where aktorid = ? and orgnummer = ?", aktorid, orgnummer).map(toNarmesteLederRelasjon).asSingle
        try {
            return session.run(selectQuery)
        } catch (e: Exception) {
            return null
        }
    }

    fun finnLederAktorer(nlid: String): List<NarmesteLederRelasjon> {
        val selectQuery = queryOf("select * from narmesteleder where nlaktorid = ?", nlid).map(toNarmesteLederRelasjon).asList
        try {
            return session.run(selectQuery)
        } catch (e: Exception) {
            return emptyList()
        }
    }

}
