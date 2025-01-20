package ch.ngns.or.vault.services.storage

import java.io.InputStream
import java.io.OutputStream
import java.util.*

interface VaultService {

    fun storeObject(data: InputStream) : UUID

    fun retrieveObject(uuid: UUID, outputStream: OutputStream)

    fun streamObjectAsPipedInputStream(uuid: UUID, outputStream: OutputStream): InputStream

    fun purgeObject(uuid: UUID)

}