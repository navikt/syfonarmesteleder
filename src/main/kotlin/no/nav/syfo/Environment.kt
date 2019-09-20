package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/secrets.json"
const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"
const val defaultlocalEnvironmentPropertiesPath = "./src/main/resources/localEnvForTests.json"
private val environmentMapper: ObjectMapper = ObjectMapper()

fun getEnvironment(): Environment {
    environmentMapper.registerKotlinModule()
    return if (appIsRunningLocally) {
        environmentMapper.readValue(firstExistingFile(localEnvironmentPropertiesPath, defaultlocalEnvironmentPropertiesPath))
    } else {
        Environment()
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

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
    val clientid: String = getEnvVar("CLIENT_ID"),
    val credentials: VaultCredentials = objectMapper.readValue(
        File(vaultApplicationPropertiesPath).readText(),
        VaultCredentials::class.java
    ),
    val databaseName: String = getEnvVar("DATABASE_NAME", "syfonarmesteleder"),
    val syfonarmestelederDBURL: String = getEnvVar("SYFONARMESTELEDER_DB_URL"),
    val vaultPostgresPath: String = getEnvVar("VAULT_POSTGRES_PATH"),
    val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD")
)

data class VaultCredentials(
    val clientsecret: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun firstExistingFile(vararg paths: String) = paths
    .map(::File)
    .first(File::exists)
