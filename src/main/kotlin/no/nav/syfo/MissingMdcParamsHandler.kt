package no.nav.syfo

import org.slf4j.LoggerFactory
import java.util.*

private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun missingCallId(api: String): String {
    log.warn("Nav-Callid har blitt fjernet før kall til {} - skal ikke skje", api)
    return UUID.randomUUID().toString()
}

fun missingConsumerId(api: String): String {
    log.warn("Nav-Consumer-Id har blitt fjernet før kall til {} - skal ikke skje", api)
    return "syfonarmesteleder"
}
