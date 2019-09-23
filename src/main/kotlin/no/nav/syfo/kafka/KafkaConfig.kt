package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.deser.std.StringDeserializer
import kafka.server.KafkaConfig
import no.nav.syfo.Environment
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.Properties

fun loadBaseConfig(env: Environment): Properties = Properties().also {
    it.load(KafkaConfig::class.java.getResourceAsStream("/kafka_base.properties"))
    it["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${env.credentials.serviceuserUsername}\" password=\"${env.credentials.serviceuserPassword}\";"
    it["bootstrap.servers"] = env.kafkaBootstrapServers
    it["specific.avro.reader"] = true
}

fun Properties.toConsumerConfig(): Properties = Properties().also {
    it.putAll(this)
    it[ConsumerConfig.GROUP_ID_CONFIG] = "syfonarmesteleder-consumer"
    it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1000"
}
