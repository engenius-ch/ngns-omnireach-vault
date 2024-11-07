package ch.ngns.or.vault.services.util

import jakarta.activation.MimetypesFileTypeMap
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

val CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv")
val ZIP_MEDIA_TYPE = MediaType.parseMediaType("application/zip")

object ResponseEntityUtil {

    fun createResponseEntity(
        file: ByteArray,
        fileName: String,
        noDownload: Boolean = false,
        mediaType: MediaType? = null
    ): ResponseEntity<ByteArray> {
        val contentType = mediaType
            ?: when {
                isPdf(file) -> MediaType.APPLICATION_PDF
                isImage(file) -> getImageContentType(file)
                else -> MediaType.APPLICATION_OCTET_STREAM
            }

        return buildResponse(file, contentType, fileName, noDownload)
    }

    private val fileTypeMap = MimetypesFileTypeMap()

    fun createStreamingResponseEntity(
        fileName: String,
        fileSize: Long? = null,
        noDownload: Boolean = false,
        mediaType: MediaType? = null,
        body: StreamingResponseBody
    ): ResponseEntity<StreamingResponseBody> {
        val contentType = mediaType
            ?: MediaType.parseMediaType(fileTypeMap.getContentType(fileName))

        val safeBody = StreamingResponseBody {
            it.use { os -> body.writeTo(os) }
        }

        return buildResponse(safeBody, contentType, fileName, noDownload, fileSize)
    }

    private fun <T> buildResponse(
        body: T,
        contentType: MediaType,
        fileName: String,
        noDownload: Boolean,
        contentSize: Long? = null
    ): ResponseEntity<T> {
        val responseEntityBuilder = ResponseEntity.ok().contentType(contentType)

        if (!noDownload) responseEntityBuilder.header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"$fileName\""
        )

        if (contentSize != null) {
            responseEntityBuilder.contentLength(contentSize)
        }

        return responseEntityBuilder.body(body)
    }

    private fun assureFormat(file: ByteArray, format: String): Boolean {
        val fileMimeType = FileUtil.detectDocTypeUsingDetector(ByteArrayInputStream(file))
        return fileMimeType == format
    }

    private fun isPdf(file: ByteArray): Boolean {
        return assureFormat(file, "application/pdf")
    }

    private fun isImage(file: ByteArray): Boolean {
        return try {
            ImageIO.read(ByteArrayInputStream(file)) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun getImageContentType(file: ByteArray): MediaType {
        val imageReaders = ImageIO.getImageReaders(ImageIO.createImageInputStream(ByteArrayInputStream(file)))

        return if (imageReaders.hasNext()) {
            val formatName = imageReaders.next().formatName
            when (formatName?.lowercase()) {
                null -> MediaType.APPLICATION_OCTET_STREAM
                "jpeg" -> MediaType.IMAGE_JPEG
                "png" -> MediaType.IMAGE_PNG
                "gif" -> MediaType.IMAGE_GIF
                else -> MediaType.APPLICATION_OCTET_STREAM
            }
        } else {
            MediaType.APPLICATION_OCTET_STREAM
        }
    }
}