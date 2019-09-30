package no.nav.syfo.syfoservice

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.syfo.NARMESTE_LEDER_TOPIC
import no.nav.syfo.objectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.syfonarmesteleder")

fun Routing.registerSyfoserviceApi(kafkaProducer: KafkaProducer<String, String>) =
    post("/syfoservice/test") {
        val narmesteLeder = call.receive<NarmesteLederDTO>()
        kafkaProducer.leggNarmesteLederPakafka(narmesteLeder)
        call.respond(HttpStatusCode.OK)
    }

fun KafkaProducer<String, String>.leggNarmesteLederPakafka(narmesteLederDTO: NarmesteLederDTO) {
    log.info("Legger narmesteleder dto med id: ${narmesteLederDTO.narmesteLederId} på topic: $NARMESTE_LEDER_TOPIC")
    send(
        ProducerRecord(
            NARMESTE_LEDER_TOPIC,
            narmesteLederDTO.narmesteLederId.toString(),
            objectMapper.writeValueAsString(narmesteLederDTO)
        )
    )
}
