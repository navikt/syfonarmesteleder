package no.nav.syfo

import com.google.gson.Gson
import java.io.File

data class Environment(
        val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
        val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "4").toInt(),
        val credentials: VaultCredentials = Gson().fromJson(readFileDirectlyAsText(vaultApplicationPropertiesPath), VaultCredentials::class.java)
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/secrets.json"

data class VaultCredentials(
        val clientId: String = getEnvVar("clientid"),
        val clientSecret: String = getEnvVar("clientsecret")
)

fun readFileDirectlyAsText(fileName: String): String
        = File(fileName).readText(Charsets.UTF_8)
