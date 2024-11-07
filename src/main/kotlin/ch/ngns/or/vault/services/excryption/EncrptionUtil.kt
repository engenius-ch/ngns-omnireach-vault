package ch.ngns.or.vault.services.excryption

import ch.ngns.or.vault.services.config.ORVaultProperties
import org.springframework.stereotype.Component

@Component
class EncrptionUtil(
    private val orVaultProperties: ORVaultProperties
) {
    fun getSalt(): String? {
        return orVaultProperties.encryptionSalt
    }
}

