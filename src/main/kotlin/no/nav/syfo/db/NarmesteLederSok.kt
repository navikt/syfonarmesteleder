package no.nav.syfo.db

import com.google.gson.GsonBuilder
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Route.registrerNarmesteLederSok(nldb: NlDb) {
    get("/syfonarmesteleder/narmesteleder") {
        val request = call.request
        try {
            MDC.put("Nav-Callid", request.header("Nav-Callid") ?: UUID.randomUUID().toString())
            MDC.put("Nav-Consumer-Id", request.header("Nav-Consumer-Id") ?: "syfonarmesteleder")

            val queryParameters: Parameters = request.queryParameters
            val aktorId: String = queryParameters["aktorId"]?.takeIf { it.isNotEmpty() } ?: ""
            val nlid = nldb.finnAktorLeder(aktorId)

            call.respond(HttpStatusCode.Accepted, nlid)

        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, e.message!!)
        } finally {
            MDC.remove("Nav-Callid")
            MDC.remove("Nav-Consumer-Id")
        }
    }
    get("/syfonarmesteleder/lederssykmeldte") {
        val request = call.request
        try {
            MDC.put("Nav-Callid", request.header("Nav-Callid") ?: UUID.randomUUID().toString())
            MDC.put("Nav-Consumer-Id", request.header("Nav-Consumer-Id") ?: "syfonarmesteleder")

            val queryParameters: Parameters = request.queryParameters
            val nlId: String = queryParameters["nlId"]?.takeIf { it.isNotEmpty() } ?: ""
            val aktorer = nldb.finnLederAktorer(nlId)
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonPersonList: String = gson.toJson(aktorer)

            call.respond(HttpStatusCode.Accepted, jsonPersonList)

        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, e.message!!)
        } finally {
            MDC.remove("Nav-Callid")
            MDC.remove("Nav-Consumer-Id")
        }
    }
}

