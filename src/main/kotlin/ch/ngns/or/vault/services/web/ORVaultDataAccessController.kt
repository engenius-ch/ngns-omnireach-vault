package ch.ngns.or.vault.services.web

import ch.ngns.or.vault.services.excryption.EncryptionService
import ch.ngns.or.vault.services.excryption.EncryptionUtil
import ch.ngns.or.vault.services.storage.VaultService
import ch.ngns.or.vault.services.util.ResponseEntityUtil
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.*
import kotlin.concurrent.thread

@RestController
@Tag(description = "Store and retreive data from the vault", name = "Vault Controller")
@Component
class ORVaultController(
    private val encryptionUtil: EncryptionUtil,
    private val encryptionService: EncryptionService,
    private val vaultService: VaultService,
) {

    private val logger = LoggerFactory.getLogger(ORVaultController::class.java)

    @GetMapping("/{id}")
    fun downloadFileStreaming(
        @RequestHeader(name = "X-Encryption-Key", required = false) encryptionKey: String?,
        @PathVariable(name = "id") id: String,
        @RequestParam(name = "noDownload", required = false, defaultValue = "false") noDownload: Boolean
    ): ResponseEntity<StreamingResponseBody> =
        ResponseEntityUtil.createStreamingResponseEntity(id, null, noDownload) { outputStream: OutputStream ->
            val uuid = UUID.fromString(id)
            if (encryptionKey.isNullOrEmpty()) {
                vaultService.retrieveObject(uuid, outputStream)
            } else {
                val encOutputStream = PipedOutputStream()
                var rawInputStream = PipedInputStream(encOutputStream)

                thread {
                    encOutputStream.use { output ->
                        vaultService.retrieveObject(uuid, output)
                    }
                }

                rawInputStream.use { inputStream ->
                    encryptionService.decodeBase64UnzipAndDecryptStream(inputStream, outputStream, encryptionKey)
                }
            }
        }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestHeader(name = "X-Encryption-Key", required = false) encryptionKey: String?,
        @RequestParam file: MultipartFile
    ): UUID? =
        encryptionKey?.let { encKey ->
            logger.debug("encryptionKey: $encKey")
            val encOutputStream = PipedOutputStream()
            val encryptedInputStream = PipedInputStream(encOutputStream)
            thread {
                try {
                    encryptionService.zipEncryptAndBase64EncodeStream(file.inputStream, encOutputStream, encKey)
                } finally {
                    encOutputStream.close() // Ensure the stream is closed to avoid hanging
                }
            }
            vaultService.storeObject(encryptedInputStream)
        } ?: vaultService.storeObject(file.inputStream)

}