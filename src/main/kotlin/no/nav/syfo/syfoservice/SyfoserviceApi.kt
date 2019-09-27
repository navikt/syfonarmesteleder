package no.nav.syfo.syfoservice

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.routing.Routing
import io.ktor.routing.post
import no.nav.syfo.NARMESTE_LEDER_TOPIC
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

fun Routing.registerSyfoserviceApi(kafkaProducer: KafkaProducer<String, NarmesteLederDTO>) =
    post("/syfoservice/test") {
        val narmesteLeder = call.receive<NarmesteLederDTO>()
        kafkaProducer.leggNarmesteLederPakafka(narmesteLeder)
    }

fun KafkaProducer<String, NarmesteLederDTO>.leggNarmesteLederPakafka(narmesteLederDTO: NarmesteLederDTO) {
    send(
        ProducerRecord(
            NARMESTE_LEDER_TOPIC,
            narmesteLederDTO.narmesteLederId.toString(),
            narmesteLederDTO
        )
    )
}
