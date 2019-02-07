package no.nav.syfo

import com.google.gson.Gson
import java.io.File

const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/secrets.json"
const val localEnvironmentPropertiesPath = "./src/main/resources/localEnvForTests.json"

fun getEnvironment(): Environment {
    return if (appIsRunningLocally) {
        Gson().fromJson(readFileDirectlyAsText(localEnvironmentPropertiesPath), Environment::class.java)
    } else {
        Environment(
                getEnvVar("APPLICATION_PORT", "8080").toInt(),
                getEnvVar("APPLICATION_THREADS", "4").toInt(),
                getEnvVar("SERVICESTRANGLER_URL"),
                getEnvVar("SERVICESTRANGLER_ID"),
                getEnvVar("AADACCESSTOKEN_URL"),
                getEnvVar("CLIENT_ID"),
                Gson().fromJson(readFileDirectlyAsText(vaultApplicationPropertiesPath), VaultCredentials::class.java)
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
        val applicationPort: Int,
        val applicationThreads: Int,
        val servicestranglerUrl: String,
        val servicestranglerId: String,
        val aadAccessTokenUrl: String,
        val clientid: String,
        val credentials: VaultCredentials
)

data class VaultCredentials(
        val clientsecret: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun readFileDirectlyAsText(fileName: String): String = File(fileName).readText(Charsets.UTF_8)
