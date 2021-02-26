package no.nav.syfo

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "4").toInt(),
    val servicestranglerUrl: String = getEnvVar("SERVICESTRANGLER_URL", "http://syfoservicestrangler"),
    val servicestranglerId: String = getEnvVar("SERVICESTRANGLER_ID"),
    val arbeidsgivertilgangId: String = getEnvVar("ARBEIDSGIVERTILGANG_ID"),
    val aadAccessTokenUrl: String = getEnvVar("AADACCESSTOKEN_URL"),
    val aadDiscoveryUrl: String = getEnvVar("AADDISCOVERY_URL"),
    val jwkKeysUrl: String = getEnvVar("JWKKEYS_URL", "https://login.microsoftonline.com/common/discovery/keys"),
    val jwtIssuer: String = getEnvVar("JWT_ISSUER"),
    val syfosoknadId: String = getEnvVar("SYFOSOKNAD_ID"),
    val syfovarselId: String = getEnvVar("SYFOVARSEL_ID"),
    val modiasyforestId: String = getEnvVar("MODIASYFOREST_ID"),
    val syfobrukertilgangId: String = getEnvVar("SYFOBRUKERTILGANG_ID"),
    val syfomoteadminId: String = getEnvVar("SYFOMOTEADMIN_ID"),
    val syfooppfolgingsplanserviceId: String = getEnvVar("SYFOOPPFOLGINGSPLANSERVICE_ID"),
    val syfosmaltinnId: String = getEnvVar("SYFOSMALTINN_ID"),
    val databaseName: String = getEnvVar("DATABASE_NAME", "syfonarmesteleder"),
    val syfonarmestelederDBURL: String = getEnvVar("SYFONARMESTELEDER_DB_URL"),
    val mountPathVault: String = getEnvVar("MOUNT_PATH_VAULT"),
    val pdlGraphqlPath: String = getEnvVar("PDL_GRAPHQL_PATH"),
    val stsUrl: String = getEnvVar("STS_URL", "http://security-token-service/rest/v1/sts/token")
)

data class VaultSecrets(
    val clientId: String = getFileAsString("/secrets/azuread/syfonarmesteleder/client_id"),
    val clientSecret: String = getFileAsString("/secrets/azuread/syfonarmesteleder/client_secret"),
    val serviceuserUsername: String = getFileAsString("/secrets/serviceuser/username"),
    val serviceuserPassword: String = getFileAsString("/secrets/serviceuser/password")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun getFileAsString(filePath: String) = String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
