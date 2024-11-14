package ch.ngns.or.vault.services.excryption

import org.springframework.stereotype.Service
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

@Service
class EncryptionService(
    val encryptionUtil: EncryptionUtil
) {

    fun zipEncryptAndBase64EncodeStream(inputStream: InputStream, outputStream: OutputStream, passphrase: String) {
        val cipher = encryptionUtil.getInitializedCipherInstanceForEncryption(passphrase)

        Base64.getEncoder().wrap(outputStream).use { base64OutputStream ->
            CipherOutputStream(base64OutputStream, cipher).use { cipherOutputStream ->
                GZIPOutputStream(cipherOutputStream).use { gzipOutputStream ->
                    inputStream.copyTo(gzipOutputStream)  // Daten zippen, verschlüsseln und Base64-encoden
                }
            }
        }
    }

    fun decodeBase64UnzipAndDecryptStream(inputStream: InputStream, outputStream: OutputStream, passphrase: String) {
        val cipher = encryptionUtil.getInitializedCipherInstanceForDecryption(passphrase)

        inputStream.use { inputStream ->
            Base64.getDecoder().wrap(inputStream).use { base64InputStream ->
                // Schritt 3: Entschlüsselung
                CipherInputStream(base64InputStream, cipher).use { cipherInputStream ->
                    GZIPInputStream(cipherInputStream).use { gzipInputStream ->
                        outputStream.use { fileOutputStream ->
                            gzipInputStream.copyTo(fileOutputStream)
                        }
                    }
                }
            }
        }
    }

}