package ch.ngns.or.vault.services.model

import jakarta.persistence.*
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime
import java.util.*


@Entity
@Table(name = "ORVAULT_OBJECTS")
data class DbVaultObject(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Lob
    @Column(name = "data")
    var data: ByteArray? = null,

    @Column(name = "has_segments")
    var segmented: Boolean = false,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(
        columnDefinition = "timestamp default now()",
        nullable = false,
        name = "created_at"
    )
    var createdAt: LocalDateTime? = null,

    @Column(name = "size")
    var size: Long = 0
) {
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DbVaultObject

        if (id != other.id) return false
        if (segmented != other.segmented) return false
        if (createdAt != other.createdAt) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + segmented.hashCode()
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + size.hashCode()
        return result
    }

}