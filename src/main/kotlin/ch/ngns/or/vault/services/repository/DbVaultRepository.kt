package ch.ngns.or.vault.services.repository

import ch.ngns.or.vault.services.model.DbVaultObject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DbVaultRepository : JpaRepository<DbVaultObject, UUID> {
}