package no.nav.syfo.testutils

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.server.testing.TestApplicationEngine
import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.application.setupAuth
import no.nav.syfo.log
import java.nio.file.Paths
import java.util.UUID

fun TestApplicationEngine.setUpTestApplication() {
    start(true)
    application.install(CallId) {
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }
    application.install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            log.error("Caught exception", cause)
        }
    }
    application.install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}

fun TestApplicationEngine.setUpAuth(): Environment {
    val env = Environment(
        servicestranglerUrl = "",
        servicestranglerId = "strangler",
        arbeidsgivertilgangId = "arbeidsgiver",
        aadAccessTokenUrl = "",
        aadDiscoveryUrl = "",
        jwtIssuer = "issuer",
        syfosoknadId = "syfosoknad",
        syfovarselId = "syfovarsel",
        modiasyforestId = "modiasyforest",
        syfobrukertilgangId = "syfobrukertilgang",
        syfomoteadminId = "moteadmin",
        syfooppfolgingsplanserviceId = "oppfolgingsplanservice",
        syfosmaltinnId = "syfosmaltinn",
        pdlGraphqlPath = "graphql",
        stsUrl = "http://sts",
        sykmeldingerBackendId = "sykmeldinger-backend",
        databaseUsername = "",
        databasePassword = "",
        dbHost = "",
        dbPort = "",
        dbName = ""
    )

    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    val jwkProvider = JwkProviderBuilder(uri).build()
    val vaultSecrets = VaultSecrets("syfonarmesteleder", "", "srvsyfonarmesteleder", "pwd")

    application.setupAuth(jwkProvider, env, vaultSecrets)
    return env
}
