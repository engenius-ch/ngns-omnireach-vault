package ch.ngns.or.vault.services.model

import jakarta.persistence.*
import java.io.Serializable
import java.util.*

@Entity
@Table(name = "ORVAULT_OBJECT_SEGMENTS")
@IdClass(DbVaultObjectSegment.ObjectSegmentId::class)
class DbVaultObjectSegment {
    @Id
    @Column(name = "object_id")
    var dbVaultObjectId: UUID? = null

    @Id
    @Column(name = "segment_index")
    var segmentIndex: Int = 0

    @Lob
    @Column(name = "segment_data", nullable = false)
    var segmentData: ByteArray

    constructor(objectId: UUID?, segmentIndex: Int, segmentData: ByteArray) {
        this.dbVaultObjectId = objectId
        this.segmentIndex = segmentIndex
        this.segmentData = segmentData
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as DbVaultObjectSegment
        return segmentIndex == that.segmentIndex && dbVaultObjectId == that.dbVaultObjectId && segmentData.contentEquals(that.segmentData)
    }

    override fun hashCode(): Int {
        var result = Objects.hash(dbVaultObjectId, segmentIndex)
        result = 31 * result + segmentData.contentHashCode()
        return result
    }

    class ObjectSegmentId (
        private var dbVaultObjectId: UUID? = null,
        private var segmentIndex : Int = 0,
    ): Serializable {


        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as ObjectSegmentId
            return segmentIndex == that.segmentIndex && dbVaultObjectId == that.dbVaultObjectId
        }

        override fun hashCode(): Int {
            return Objects.hash(dbVaultObjectId, segmentIndex)
        }
    }
}