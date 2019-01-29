package no.nav.syfo

import com.google.gson.Gson
import java.io.File

const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/secrets.json"
const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"

fun getEnvironment(): Environment {
    return if (appIsRunningLocally) {
        val localEnv = Gson().fromJson(readFileDirectlyAsText(localEnvironmentPropertiesPath), LocalEnvironment::class.java)
        Environment(
                localEnv.applicationPort,
                localEnv.applicationThreads,
                localEnv.servicestranglerUrl,
                VaultCredentials(localEnv.clientid, localEnv.clientsecret)
        )
    } else {
        Environment(
                getEnvVar("APPLICATION_PORT", "8080").toInt(),
                getEnvVar("APPLICATION_THREADS", "4").toInt(),
                getEnvVar("SERVICESTRANGLER_URL"),
                Gson().fromJson(readFileDirectlyAsText(vaultApplicationPropertiesPath), VaultCredentials::class.java)
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
        val applicationPort: Int,
        val applicationThreads: Int,
        val servicestranglerUrl: String,
        val credentials: VaultCredentials
)

data class VaultCredentials(
        val clientid: String,
        val clientsecret: String
)

private data class LocalEnvironment(
        val applicationPort: Int,
        val applicationThreads: Int,
        val servicestranglerUrl: String,
        val clientid: String,
        val clientsecret: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun readFileDirectlyAsText(fileName: String): String = File(fileName).readText(Charsets.UTF_8)
