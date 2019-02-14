package no.nav.syfo.traceinterceptor

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.header
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelineInterceptor
import org.slf4j.MDC
import java.util.*

suspend fun <T : Any> PipelineContext<T, ApplicationCall>.withTraceInterceptor(body: PipelineInterceptor<T, ApplicationCall>) {
    try {
        MDC.put("Nav-Callid", call.request.header("Nav-Callid") ?: UUID.randomUUID().toString())
        MDC.put("Nav-Consumer-Id", call.request.header("Nav-Consumer-Id") ?: "syfohrtilgang")

        body(subject)
    } finally {
        MDC.remove("Nav-Callid")
        MDC.remove("Nav-Consumer-Id")
    }
}