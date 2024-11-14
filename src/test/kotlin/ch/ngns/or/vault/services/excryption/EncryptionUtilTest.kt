package ch.ngns.or.vault.services.excryption

import ch.ngns.or.vault.services.config.ORVaultProperties
import kotlin.concurrent.thread
import org.assertj.core.util.Files
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey


class EncryptionUtilTest {

    //@Autowired
    lateinit var encryptionUtil: EncryptionUtil
    lateinit var encryptionService: EncryptionService

    val TEST_SALT : String = "orsalt"
    val TEST_PW : String = "passwrd"
    val ENC_KEY_LENGTH : Int = 32

    @BeforeEach
    fun setUp() {
        encryptionUtil = EncryptionUtil(
            ORVaultProperties(TEST_SALT, ENC_KEY_LENGTH)
        )
        encryptionService = EncryptionService(encryptionUtil)
    }

    @Test
    fun getSalt() {
        assertEquals(encryptionUtil.getSalt(),TEST_SALT);
    }

    @Test
    fun encrypt() {
        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest_plain.txt")
        val outputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        //outputStream.encodingWith(Base64.Default)
        encryptionUtil.encrypt(inputStream, outputStream, encryptionUtil.createKeyFromString(encryptionUtil.getSalt() + TEST_PW))
        System.out.println("Encrypted Data: ")
        println("---")
        System.out.flush()
        System.out.use { fileOutputStream ->
            // Step 3: Wrap the FileOutputStream in a Base64 Encoder
            Base64.getEncoder().wrap(fileOutputStream).use { base64OutputStream ->
                // Step 4: Write the data from ByteArrayOutputStream to the Base64 OutputStream
                outputStream.writeTo(base64OutputStream)
            }
        }
        System.out.flush()
        println("")
        println("---")
    }

    @Test
    fun decrypt() {
        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest_enc.txt")
        val outputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        //outputStream.encodingWith(Base64.Default)
        val decodedInputStream: InputStream = Base64.getDecoder().wrap(inputStream)
        encryptionUtil.decrypt(decodedInputStream, outputStream, encryptionUtil.createKeyFromString(encryptionUtil.getSalt() + TEST_PW))
        System.out.println("Decrypted Data: ")
        println("---")
        System.out.flush()
        System.out.use { fileOutputStream ->
            // Step 3: Wrap the FileOutputStream in a Base64 Encoder
            outputStream.writeTo(fileOutputStream)
        }
        System.out.flush()
        println("")
        println("---")
    }

    @Test
    fun byteArrayToBase64() {
        val byteArray = "This is a test.".toByteArray()
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.write(byteArray)
        println("---")
        System.out.use { fileOutputStream ->
            // Step 3: Wrap the FileOutputStream in a Base64 Encoder
            Base64.getEncoder().wrap(fileOutputStream).use { base64OutputStream ->
                // Step 4: Write the data from ByteArrayOutputStream to the Base64 OutputStream
                byteArrayOutputStream.writeTo(base64OutputStream)
            }
        }
        System.out.flush()
        println("")
        println("---")

        //System.out.println(Base64.getEncoder().encodeToString())
    }

    @Test
    fun base64ToByteArray() {
        val base64ByteArray = "VGhpcyBpcyBhIHRlc3Qu".toByteArray()
        val base64ByteArrayInputStream = ByteArrayInputStream(base64ByteArray)
        //base64ByteArrayInputStream.write(base64ByteArray)
        println("---")
        System.out.flush()
        val decodedInputStream: InputStream = Base64.getDecoder().wrap(base64ByteArrayInputStream)

        decodedInputStream.copyTo(System.out)

        System.out.flush()
        println("")
        println("---")
    }

    @Test
    fun testEncrypt() {
        val dataToEncrypt = "This is a test.".toByteArray(Charsets.UTF_8)
        val byteArrayInputStream = ByteArrayInputStream(dataToEncrypt)

        // Step 2: Set up AES encryption (use a 128-bit key for this example)
        val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES algorithm with ECB mode and PKCS5 padding
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // Step 3: Set up a CipherInputStream to stream the encrypted data
        val cipherInputStream: InputStream = CipherInputStream(byteArrayInputStream, cipher)

        // Step 4: Base64 encode the encrypted data as it is read from the CipherInputStream
        println("---")
        System.out.flush()
        Base64.getEncoder().wrap(System.out).use { base64OutputStream ->
            cipherInputStream.copyTo(base64OutputStream)  // Stream encrypted data and Base64 encode it
        }
        System.out.flush()
        println("")
        println("---")
    }

    @Test
    fun testDecrypt() {
        // Example Base64-encoded and encrypted data
        val base64EncryptedData = "Yd+XV2J4eswCn7pL7WGiCw==" // Fake Base64 + AES encrypted data (you can replace with actual)

        // Step 1: Create a ByteArrayInputStream with Base64-encoded encrypted data
        val base64ByteArrayInputStream = ByteArrayInputStream(base64EncryptedData.toByteArray(Charsets.UTF_8))

        // Step 2: Decode the Base64-encoded encrypted data
        val decodedInputStream = Base64.getDecoder().wrap(base64ByteArrayInputStream)

        // Step 3: Set up AES decryption (same secret key used during encryption)
        val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES decryption in ECB mode
        cipher.init(Cipher.DECRYPT_MODE, secretKey)

        // Step 4: Decrypt the Base64-decoded data using CipherInputStream
        val cipherInputStream: InputStream = CipherInputStream(decodedInputStream, cipher)

        // Step 5: Copy decrypted data to the output (e.g., to System.out or a file)
        cipherInputStream.copyTo(System.out)

        println()  // Print a newline after the decrypted output
    }

    @Test
    fun testEncryptBase64Large() {
        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest-large_plain.txt")

        // Step 2: Set up AES encryption (use a 128-bit key for this example)
        val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES algorithm with ECB mode and PKCS5 padding
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // Step 3: Set up a CipherInputStream to stream the encrypted data
        val cipherInputStream: InputStream = CipherInputStream(inputStream, cipher)

        val outFile = Files.newFile(Files.temporaryFolderPath().plus("EncryptBase64-").plus(UUID.randomUUID()).plus(".txt"))
        val fileOutputStream = FileOutputStream(outFile)
        Base64.getEncoder().wrap(fileOutputStream).use { base64OutputStream ->
            cipherInputStream.copyTo(base64OutputStream)  // Stream encrypted data and Base64 encode it
        }
        System.out.flush()
        println("Encrypted file written to: ${outFile.absolutePath}")
    }
    @Test
    fun testEncryptZipBase64() {
        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest-large_plain.txt")

        // Step 2: Set up AES encryption (use a 128-bit key for this example)
        val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES algorithm with ECB mode and PKCS5 padding
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // Step 3: Set up a CipherInputStream to stream the encrypted data
        val cipherInputStream: InputStream = CipherInputStream(inputStream, cipher)

        // Step 4: Base64 encode the encrypted data as it is read from the CipherInputStream
//        val boz = ByteArrayOutputStream()
//        val zOutputStream = GZIPOutputStream(boz).use { gzOutStream ->
//            cipherInputStream.copyTo(gzOutStream)
//        }
//
        val outFile = Files.newFile(Files.temporaryFolderPath().plus("EncryptZipBase64-").plus(UUID.randomUUID()).plus(".txt"))
        val fileOutputStream = FileOutputStream(outFile)
        GZIPOutputStream(fileOutputStream).use { gzipOutputStream ->
            Base64.getEncoder().wrap(gzipOutputStream).use { base64OutputStream ->
                cipherInputStream.copyTo(base64OutputStream)  // Stream encrypted data and Base64 encode it
            }
        }
        println("Encrypted file written to: ${outFile.absolutePath}")
    }

    @Test
    fun testEncryptBase64Zip() {
        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest-large_plain.txt")

        // Step 2: Set up AES encryption (use a 128-bit key for this example)
        val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES algorithm with ECB mode and PKCS5 padding
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // Step 3: Set up a CipherInputStream to stream the encrypted data
        val cipherInputStream: InputStream = CipherInputStream(inputStream, cipher)

        val outFile = Files.newFile(Files.temporaryFolderPath().plus("EncryptBase64Zip-").plus(UUID.randomUUID()).plus(".txt"))
        val fileOutputStream = FileOutputStream(outFile)
        Base64.getEncoder().wrap(fileOutputStream).use { base64OutputStream ->
            GZIPOutputStream(base64OutputStream).use { gzipOutputStream ->
                cipherInputStream.copyTo(gzipOutputStream)  // Stream encrypted data and Base64 encode it
            }
        }
        println("Encrypted file written to: ${outFile.absolutePath}")
    }

    @Test
    fun testEncryptLargeNoZipNoBase64() {
        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest-large_plain.txt")

        // Step 2: Set up AES encryption (use a 128-bit key for this example)
        val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES algorithm with ECB mode and PKCS5 padding
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // Step 3: Set up a CipherInputStream to stream the encrypted data
        val cipherInputStream: InputStream = CipherInputStream(inputStream, cipher)

        val outFile = Files.newFile(Files.temporaryFolderPath().plus("Encrypt-").plus(UUID.randomUUID()).plus(".txt"))
        FileOutputStream(outFile).use { outStream ->
            cipherInputStream.copyTo(outStream)
        }
        println("Encrypted file written to: ${outFile.absolutePath}")
    }

    @Test
    fun testZipEncryptBase64() {
        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest-large_plain.txt")
        val outFile = Files.newFile(Files.temporaryFolderPath().plus("ZipEncryptBase64-").plus(UUID.randomUUID()).plus(".txt"))
        val outputStream = FileOutputStream(outFile)


        // Step 2: Set up AES encryption (use a 128-bit key for this example)
        val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")  // AES algorithm with ECB mode and PKCS5 padding
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        Base64.getEncoder().wrap(outputStream).use { base64OutputStream ->
            CipherOutputStream(base64OutputStream, cipher).use { cipherOutputStream ->
                GZIPOutputStream(cipherOutputStream).use { gzipOutputStream ->
                    inputStream.copyTo(gzipOutputStream)  // Daten zippen, verschlüsseln und Base64-encoden
                }
            }
        }
        println("Encrypted file written to: ${outFile.absolutePath}")
    }

    @Test
    fun testBase64DecodeDecryptUnzip() {

        val inputStream: InputStream = javaClass.getResourceAsStream("/encryption/enctest-large_enc.txt")
        val outFile = Files.newFile(Files.temporaryFolderPath().plus("Base64DecodeDecryptUnzip-").plus(UUID.randomUUID()).plus(".txt"))
        val outputStream = FileOutputStream(outFile)

        inputStream.use { fileInputStream ->
            // Schritt 1: Base64-Dekodierung
            Base64.getDecoder().wrap(fileInputStream).use { base64InputStream ->
                // Schritt 2: AES-Entschlüsselung vorbereiten
                val secretKey: SecretKey = encryptionUtil.createKeyFromString("password")
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey)

                // Schritt 3: Entschlüsselung
                CipherInputStream(base64InputStream, cipher).use { cipherInputStream ->
                    // Schritt 4: Dekomprimierung
                    GZIPInputStream(cipherInputStream).use { gzipInputStream ->
                        // Entpackte und entschlüsselte Daten in eine Datei schreiben
                        outputStream.use { fileOutputStream ->
                            gzipInputStream.copyTo(fileOutputStream)
                        }
                    }
                }
            }
        }

        println("Encrypted file written to: ${outFile.absolutePath}")
    }

    @Test
    fun testEncryptionService() {
        val origRawResource = "/encryption/enctest-large_plain.txt"
        val encOutputStream: PipedOutputStream = PipedOutputStream()
        val origByteArray: ByteArray = javaClass.getResource(origRawResource).readBytes()
        val origInputStream: InputStream = ByteArrayInputStream(origByteArray)
        val decodedOutputStream = ByteArrayOutputStream()
        val encryptedInputStream = PipedInputStream(encOutputStream)

        thread {
            encOutputStream.use { output ->
                encryptionService.zipEncryptAndBase64EncodeStream(origInputStream, encOutputStream, TEST_PW)
            }
        }

        encryptedInputStream.use { input ->
            encryptionService.decodeBase64UnzipAndDecryptStream(input, decodedOutputStream, TEST_PW)
        }
        val decodedOutput = decodedOutputStream.toByteArray()
        assertArrayEquals(decodedOutput, origByteArray)

    }



}