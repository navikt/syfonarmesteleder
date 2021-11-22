package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.log

fun Application.setupAuth(jwkProvider: JwkProvider, env: Environment, vaultSecrets: VaultSecrets) {
    install(Authentication) {
        jwt {
            verifier(jwkProvider, env.jwtIssuer)
            realm = "Syfonarmesteleder"
            validate { credentials ->
                when {
                    harTilgang(credentials, env, vaultSecrets.clientId) -> JWTPrincipal(credentials.payload)
                    else -> unauthorized(credentials)
                }
            }
        }
    }
}

fun harTilgang(credentials: JWTCredential, env: Environment, clientId: String): Boolean {
    val authorizedUsers = listOf(
        env.syfobrukertilgangId,
        env.syfomoteadminId,
        env.syfooppfolgingsplanserviceId
    )
    val appid: String = credentials.payload.getClaim("appid").asString()
    log.debug("authorization attempt for $appid")
    return appid in authorizedUsers && credentials.payload.audience.contains(clientId)
}

fun unauthorized(credentials: JWTCredential): Principal? {
    log.warn(
        "Auth: Unexpected audience for jwt {}, {}",
        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
        StructuredArguments.keyValue("audience", credentials.payload.audience)
    )
    return null
}
