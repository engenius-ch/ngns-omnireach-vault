package ch.ngns.or.vault.services.excryption

import ch.ngns.or.vault.services.config.ORVaultProperties
import org.springdoc.webmvc.ui.SwaggerIndexTransformer
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.security.MessageDigest
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

    fun getInitializedCipherInstanceForEncryption(passphrase: String): Cipher = getInitializedCipherInstance(passphrase, Cipher.ENCRYPT_MODE)

    fun getInitializedCipherInstanceForDecryption(passphrase: String): Cipher = getInitializedCipherInstance(passphrase, Cipher.DECRYPT_MODE)

    fun getInitializedCipherInstance(passphrase: String, cipherMode: Int, rawKeyPassphrase : Boolean = false) : Cipher {
        val secretKey: SecretKey = createKeyFromString(passphrase, rawKeyPassphrase)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES algorithm with ECB mode and PKCS5 padding
        cipher.init(cipherMode, secretKey)
        return cipher
    }

    fun createKeyFromString(keyString: String, useRaw : Boolean = false): SecretKey {

        // if there is a salt, let's add it to the passphrase
        getSalt()?.let { keyString.plus(it) }

        val hashedKey = hashStringThreadSafe(keyString)
        val keyBytes = if (useRaw) {keyString.toByteArray(Charsets.UTF_8)} else hashedKey.toByteArray(Charsets.UTF_8)

        // Schlüssel auf 16, 24 oder 32 Bytes bringen (für AES erforderlich)
        val keyLength = orVaultProperties.encryptionKeyLength!! // 256-Bit AES (32 Byte)
        val keyPadded = ByteArray(keyLength!!)

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

    private val threadLocalDigest: ThreadLocal<MessageDigest> = ThreadLocal.withInitial {
        MessageDigest.getInstance("SHA-256")
    }

    fun hashStringThreadSafe(input: String): String {
        val bytes = input.toByteArray()
        val md = threadLocalDigest.get()  // Each thread gets its own MessageDigest instance
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

