package no.nav.syfo.db

import kotliquery.*
import java.time.ZonedDateTime

data class Aktor(val aktorid: String, val nlid: String, val created: ZonedDateTime = ZonedDateTime.now())

val toAktor: (Row) -> Aktor = { row ->
    Aktor(
            row.string("aktorid"),
            row.string("nlid"),
            row.zonedDateTime("created")
    )
}

private fun visAktorer(aktorer: List<Aktor>?) {
    println("visAktorer: ")
    aktorer?.forEach {
        println(it.toString())
        println(it.aktorid)
        println(it.nlid)
        println(it.created)
    }
}


class NlDb {
    val dbname = "nltest"
    val dbuser = "user"
    val dbpass = "pass"
    val session: Session = connect()


    private fun connect(): Session {
        return createDb()
    }

    private fun createDb(): Session {
        val connstr = "jdbc:h2:mem:" + dbname + ";DB_CLOSE_DELAY=-1"
        println("createDb() " + connstr)
        /*
        Flyway.configure().run {
            dataSource("jdbc:h2:mem:" + dbname, dbuser, dbpass)
            load().migrate()
        }*/

        val tblQueryString = "create table aktor(" +
                "aktorid varchar(64) not null primary key," +
                "nlid varchar(64) not null," +
                "created timestamp not null)"
        val tblQuery = queryOf(tblQueryString).asExecute
        HikariCP.default(connstr, dbuser, dbpass)
        val session = sessionOf(HikariCP.dataSource())
        try {
            session.run(tblQuery);
            val insertQueryString = "insert into aktor (aktorid, nlid, created) values (?,?,?)"
            listOf("Arne", "Berit", "Cathrine", "David", "Erik", "Fredrik", "Gunn", "Hilde", "Ivar", "Thea").forEach { name ->
                session.run(queryOf(insertQueryString, name, "Jan", java.time.ZonedDateTime.now()).asUpdate)
            }
            listOf("Kevin", "Lise", "Martin", "Nina", "Oliver", "Patrick", "Quentin", "Renee", "Susanne", "Jan").forEach { name ->
                session.run(queryOf(insertQueryString, name, "Thea", java.time.ZonedDateTime.now()).asUpdate)
            }

        } catch (e: Exception) {
            println(e.toString())
        }
        return session
    }

    fun finnAktorLeder(aktorid: String): String {
        var retval = ""
        val selectQuery = queryOf("select nlid from aktor where aktorid = ?", aktorid).map { row -> row.string(1) }.asSingle
        try {
            retval = session.run(selectQuery) ?: ""
        } catch (e: Exception) {
            println(e.toString())
        }
        return retval
    }

    fun finnLederAktorer(nlid: String): List<Aktor> {
        var retval = emptyList<Aktor>()
        val selectQuery = queryOf("select * from aktor where nlid = ?", nlid).map(no.nav.syfo.db.toAktor).asList
        try {
            retval = session.run(selectQuery)
        } catch (e: Exception) {
            println(e.toString())
        }
        return retval
    }

    fun aktorList(): List<Aktor> {
        var members: List<Aktor> = emptyList()
        val selectQuery = queryOf("select aktorid,nlid,created from aktor").map(no.nav.syfo.db.toAktor).asList
        try {
            members = session.run(selectQuery)
        } catch (e: Exception) {
            println(e.toString())
        }
        return members
    }
}