package no.nav.syfo.syfoservice

import no.nav.syfo.db.DatabaseInterface
import java.sql.Timestamp

fun DatabaseInterface.leggTilNarmesteLedere(narmesteLederDAO: List<NarmesteLederDAO>) =
    connection.use { connection ->
        val statement = connection.prepareStatement(
            """
                INSERT INTO NARMESTE_LEDER(NARMESTE_LEDER_ID,
                                           ORGNUMMER,
                                           BRUKER_FNR,
                                           NARMESTE_LEDER_FNR,
                                           NARMESTE_LEDER_TELEFONNUMMER,
                                           NARMESTE_LEDER_EPOST,
                                           AKTIV_FOM,
                                           AKTIV_TOM)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """
        )

        narmesteLederDAO
            .map { narmesteLeder ->
                statement.use {
                    it.setString(1, narmesteLeder.narmesteLederId)
                    it.setString(2, narmesteLeder.orgnummer)
                    it.setString(3, narmesteLeder.brukerFnr)
                    it.setString(4, narmesteLeder.narmesteLederFnr)
                    it.setString(5, narmesteLeder.narmesteLederTelefonnummer)
                    it.setString(6, narmesteLeder.narmesteLederEpost)
                    it.setTimestamp(7, Timestamp.valueOf(narmesteLeder.aktivFom))
                    it.setTimestamp(8, Timestamp.valueOf(narmesteLeder.aktivTom))
                    it.addBatch()
                }
            }

        statement.executeBatch()
        connection.commit()
    }

fun DatabaseInterface.leggTilForskutteringer(forskutteringDAO: List<ForskutteringDAO>) =
    connection.use { connection ->
        val statement = connection.prepareStatement(
            """
                INSERT INTO FORSKUTTERING(BRUKER_FNR,
                                          ORGNUMMER,
                                          ARBEIDSGIVER_FORSKUTTERER,
                                          SIST_OPPDATERT)
                VALUES (?, ?, ?, ?);
            """
        )

        forskutteringDAO
            .filter { it.arbeidsgiverForskutterer != null }
            .map { forskuttering ->
                statement.use {
                    it.setString(1, forskuttering.brukerFnr)
                    it.setString(2, forskuttering.orgnummer)
                    it.setBoolean(3, forskuttering.arbeidsgiverForskutterer!!)
                    it.setTimestamp(4, Timestamp.valueOf(forskuttering.sistOppdatert))
                    it.addBatch()
                }
            }

        statement.executeBatch()
        connection.commit()
    }
