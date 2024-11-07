package ch.ngns.or.vault.services.storage

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

interface VaultService {
    fun storeObject(data: InputStream) : UUID?
    fun retrieveObject(uuid: UUID, outputStream: OutputStream)
    fun purgeObject(uuid: UUID)
}