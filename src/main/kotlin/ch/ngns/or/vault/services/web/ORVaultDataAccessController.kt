package ch.ngns.or.vault.services.web

import ch.ngns.or.vault.services.excryption.EncryptionService
import ch.ngns.or.vault.services.excryption.EncryptionUtil
import ch.ngns.or.vault.services.storage.VaultService
import ch.ngns.or.vault.services.util.ResponseEntityUtil
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/vault")
@Tag(description = "Store and retreive data from the vault", name = "Vault Controller")
@Component
class ORVaultController(
    val encryptionUtil: EncryptionUtil,
    val encryptionService: EncryptionService,
    val vaultService: VaultService,
) {
    private val logger = LoggerFactory.getLogger(ORVaultController::class.java)

    @GetMapping("/hello")
    @Operation(description = "simple hello world one to test the service")
    fun getHello(
        @RequestParam iam: String?
    ): String = "Hello $iam"

    @GetMapping("/salt")
    @Operation(description = "Fetch a salt")
    fun getSalt(): String? = encryptionUtil.getSalt()

    @GetMapping("/{id}/stream")
    fun downloadFileStreaming(@PathVariable id: String, @RequestParam(name = "noDownload", required = false, defaultValue = "false") noDownload: Boolean, @RequestHeader(name = "X-Encryption-Key", required = false) encryptionKey: String?): ResponseEntity<StreamingResponseBody> {
        val uuid = UUID.fromString(id)

        val pOut = PipedOutputStream()
        if (encryptionKey.isNullOrEmpty()) {
            return ResponseEntityUtil.createStreamingResponseEntity(id, null,  noDownload) { outputStream: OutputStream ->
                vaultService.retrieveObject(uuid, outputStream)
            }
        } else {
            return ResponseEntityUtil.createStreamingResponseEntity(id, null,  noDownload) { outputStream: OutputStream ->
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

    }

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(@RequestParam file: MultipartFile, @RequestHeader(name = "X-Encryption-Key", required = false) encryptionKey: String?): UUID? {
        logger.debug("encryptionKey: $encryptionKey")
        encryptionKey?.let { encKey ->
            var encOutputStream = PipedOutputStream()
            var encryptedInputStream = PipedInputStream(encOutputStream)
            thread {
                try {
                    encryptionService.zipEncryptAndBase64EncodeStream(file.inputStream, encOutputStream, encKey)
                } finally {
                    encOutputStream.close() // Ensure the stream is closed to avoid hanging
                }
            }
            return vaultService.storeObject(encryptedInputStream)
        } ?: return vaultService.storeObject(file.inputStream)
    }

    private fun pipedInputStreamFromMultipartfile(file: MultipartFile): PipedInputStream {
        val pipedInputStream = PipedInputStream()
        val pipedOutputStream = PipedOutputStream(pipedInputStream)

        // Start a new thread to pipe data from FileInputStream to PipedOutputStream
        thread {
            file.inputStream.use { fileInputStream ->
                pipedOutputStream.use { outputStream ->
                    fileInputStream.copyTo(outputStream)
                }
            }
        }

        return pipedInputStream
    }
}