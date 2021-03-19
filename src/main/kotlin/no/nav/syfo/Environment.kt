package no.nav.syfo

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
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
    val sykmeldingerBackendId: String = getEnvVar("SYKMELDINGERBACKEND_ID"),
    val databaseUsername: String = getEnvVar("NAIS_DATABASE_USERNAME"),
    val databasePassword: String = getEnvVar("NAIS_DATABASE_PASSWORD"),
    val dbHost: String = getEnvVar("NAIS_DATABASE_HOST"),
    val dbPort: String = getEnvVar("NAIS_DATABASE_PORT"),
    val dbName: String = getEnvVar("NAIS_DATABASE_DATABASE"),
    val pdlGraphqlPath: String = getEnvVar("PDL_GRAPHQL_PATH"),
    val stsUrl: String = getEnvVar("STS_URL", "http://security-token-service/rest/v1/sts/token")
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$dbHost:$dbPort/$dbName"
    }
}

data class VaultSecrets(
    val clientId: String = getFileAsString("/var/run/secrets/AZURE_CLIENT"),
    val clientSecret: String = getFileAsString("/var/run/secrets/AZURE_CLIENT_SECRET"),
    val serviceuserUsername: String = getFileAsString("/var/run/secrets/SYFONARMESTELEDER_USERNAME"),
    val serviceuserPassword: String = getFileAsString("/var/run/secrets/SYFONARMESTELEDER_PASSWORD")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun getFileAsString(filePath: String) = String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
