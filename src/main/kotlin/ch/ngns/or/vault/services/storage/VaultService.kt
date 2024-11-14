package ch.ngns.or.vault.services.storage

import ch.ngns.or.vault.services.model.DbVaultObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

interface VaultService {
    fun storeObject(data: InputStream) : UUID?
    fun retrieveObject(uuid: UUID, outputStream: OutputStream) : Unit
    fun streamObjectAsPipedInputStream(uuid: UUID, outputStream: OutputStream): InputStream
    fun purgeObject(uuid: UUID)
}