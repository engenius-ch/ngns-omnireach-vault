package ch.ngns.or.vault.services.repository

import ch.ngns.or.vault.services.model.DbVaultObject
import ch.ngns.or.vault.services.model.DbVaultObjectSegment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DbVaultObjectSegmentsRepository : JpaRepository<DbVaultObjectSegment, DbVaultObjectSegment.ObjectSegmentId> {
    @Modifying
    @Query("delete from DbVaultObjectSegment fs where fs.dbVaultObjectId = ?1")
    fun deleteInBulkByFileId(fileId: UUID)
}