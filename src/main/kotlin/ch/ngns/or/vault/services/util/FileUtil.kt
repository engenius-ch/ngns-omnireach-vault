package ch.ngns.or.vault.services.util

import org.springframework.web.multipart.MultipartFile
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.detect.Detector
import org.apache.tika.mime.MediaType
import java.io.IOException
import java.io.InputStream
import org.apache.tika.metadata.Metadata

class FileUtil {

    companion object {
        private val filenamePattern = Regex("[ 0-9_a-zA-Z%\\-.]+")

        fun extractFileName(file: MultipartFile): String {
            return file.originalFilename?.let {
                filenamePattern.findAll(it).lastOrNull()?.value
            } ?: file.name
        }

        fun formatFileName(fileName: String, extension: String): String {
            if (fileName.endsWith(".$extension")) {
                return fileName
            }
            
            return "$fileName.$extension"
        }

        @Throws(IOException::class)
        fun detectDocTypeUsingDetector(stream: InputStream?): String {
            val detector: Detector = DefaultDetector()
            val metadata = Metadata()
            val mediaType: MediaType = detector.detect(stream, metadata)
            return mediaType.toString()
        }
    }
}