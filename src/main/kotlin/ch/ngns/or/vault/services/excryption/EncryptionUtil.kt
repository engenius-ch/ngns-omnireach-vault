package ch.ngns.or.vault.services.excryption

import ch.ngns.or.vault.services.config.ORVaultProperties
import org.springdoc.webmvc.ui.SwaggerIndexTransformer
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread


@Component
class EncryptionUtil(
    private val orVaultProperties: ORVaultProperties,
) {
    fun getSalt(): String? {
        return orVaultProperties.encryptionSalt
    }

    fun createKeyFromString(keyString: String): SecretKey {
        val keyBytes = keyString.toByteArray(Charsets.UTF_8)

        // Schlüssel auf 16, 24 oder 32 Bytes bringen (für AES erforderlich)
        val keyLength = 32 // 256-Bit AES (32 Byte)
        val keyPadded = ByteArray(keyLength)

        // Bytes des Strings kopieren (oder kürzen)
        System.arraycopy(keyBytes, 0, keyPadded, 0, keyBytes.size.coerceAtMost(keyLength))

        return SecretKeySpec(keyPadded, "AES")
    }

    // Verschlüsseln eines InputStreams und in einen OutputStream schreiben
    fun encrypt(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val cipherOutputStream = CipherOutputStream(outputStream, cipher)

        // Initialisierungsvektor (IV) speichern
        outputStream.write(cipher.iv)

        thread {
            inputStream.use { input ->
                cipherOutputStream.use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun pipedInputStreamFromInputStream(inputStream: InputStream): PipedInputStream {
        val pipedInputStream = PipedInputStream()
        val pipedOutputStream = PipedOutputStream(pipedInputStream)

        // Start a new thread to pipe data from FileInputStream to PipedOutputStream
        thread {
            inputStream.use { fileInputStream ->
                pipedOutputStream.use { outputStream ->
                    fileInputStream.copyTo(outputStream)
                }
            }
        }

        return pipedInputStream
    }

    // Entschlüsseln eines InputStreams und in einen OutputStream schreiben
    fun decrypt(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        // Initialisierungsvektor (IV) aus dem InputStream lesen
        val iv = ByteArray(cipher.blockSize)
        inputStream.read(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.IvParameterSpec(iv))
        val cipherInputStream = CipherInputStream(inputStream, cipher)

        thread {
            outputStream.use { output ->
                cipherInputStream.use { input ->
                    input.copyTo(output)
                }
            }
        }
    }
}

