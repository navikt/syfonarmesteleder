package no.nav.syfo.application.vault

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.db.VaultCredentialService

class RenewVaultService(private val vaultCredentialService: VaultCredentialService, private val applicationState: ApplicationState) {
    fun startRenewTasks() {
        GlobalScope.launch {
            try {
                Vault.renewVaultTokenTask(applicationState)
            } catch (e: Exception) {
                log.error("Noe gikk galt ved fornying av vault-token", e.message)
            } finally {
                applicationState.ready = false
            }
        }

        GlobalScope.launch {
            try {
                vaultCredentialService.runRenewCredentialsTask(applicationState)
            } catch (e: Exception) {
                log.error("Noe gikk galt ved fornying av vault-credentials", e.message)
            } finally {
                applicationState.ready = false
            }
        }
    }
}
