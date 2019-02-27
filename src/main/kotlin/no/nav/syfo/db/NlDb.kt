package no.nav.syfo.db

import kotliquery.*
import no.nav.syfo.getEnvVar
import no.nav.syfo.getEnvironment
import no.nav.syfo.narmestelederapi.NarmesteLeder
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

class NlDb {
    val env= getEnvironment()
    val dbconnstr = env.dbConnString
    val dbuser = env.dbUser
    val dbpass = env.dbPass
    val session: Session = connect()

    private fun connect(): Session {
        Flyway.configure().run {
            dataSource(dbconnstr, dbuser, dbpass)
            load().migrate()
        }
        HikariCP.default(dbconnstr, dbuser, dbpass)
        val session = sessionOf(HikariCP.dataSource())
        return session
    }

    fun finnAktorLeder(aktorid: String,orgnummer: String): NarmesteLeder? {
        var retval:NarmesteLeder? = null
        val selectQuery = queryOf("select * from narmesteleder where aktorid = ? and orgnummer = ?", aktorid,orgnummer).map(toNarmesteLeder).asSingle
        try {
            retval = session.run(selectQuery)
        } catch (e: Exception) {
            println(e.toString())
        }
        return retval
    }

    fun finnLederAktorer(nlid: String): List<NarmesteLeder> {
        var retval = emptyList<NarmesteLeder>()
        val selectQuery = queryOf("select * from narmesteleder where nlaktorid = ?", nlid).map(toNarmesteLeder).asList
        try {
            retval = session.run(selectQuery)
        } catch (e: Exception) {
            println(e.toString())
        }
        return retval
    }

}
