package ch.ngns.or.vault.services.storage.db

import ch.ngns.or.vault.services.model.DbVaultObject
import ch.ngns.or.vault.services.model.DbVaultObjectSegment
import ch.ngns.or.vault.services.repository.DbVaultObjectSegmentsRepository
import ch.ngns.or.vault.services.repository.DbVaultRepository
import ch.ngns.or.vault.services.storage.VaultService
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import java.io.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Service("vaultService")
class DbVaultService (
    val dbVaultRepository: DbVaultRepository,
    val dbVaultObjectSegmentRepository: DbVaultObjectSegmentsRepository,
    @PersistenceContext private val entityManager: EntityManager,
) : VaultService {

    private val logger = LoggerFactory.getLogger(DbVaultService::class.java)
    private val executorService = Executors.newCachedThreadPool()
    private val dispatcher = executorService.asCoroutineDispatcher()

    private val defaultSegmentSize = 5_000_000
    private val defaultSegmentFetchParallelism = 10

    override fun storeObject(dataStream: InputStream): UUID? =
        dataStream.use {
            val segmentSize = getSegmentSize()
            runBlocking {
                val chunks = inputStreamToChunks(dataStream, segmentSize)

                val data = dbVaultRepository.save(DbVaultObject().apply {
                     data = chunks.receiveCatching().getOrNull() ?: return@runBlocking null
                })

                var index = 0
                for (chunk in chunks) {
                    data.segmented = true
                    data.size += chunk.size
                    val segment = DbVaultObjectSegment(data.id, index++, chunk)
                    entityManager.persist(segment)
                    entityManager.flush()
                    entityManager.detach(segment)
                }
                data.id
            }
    }

    override fun retrieveObject(uuid: UUID,  outputStream: OutputStream) {
        val dbVaultObject = dbVaultRepository.findByIdOrNull(uuid) ?: throw FileNotFoundException()
        streamObject(dbVaultObject, outputStream)
    }

    override fun purgeObject(uuid: UUID) {
        try {
            dbVaultObjectSegmentRepository.deleteInBulkByFileId(uuid)
            dbVaultRepository.deleteById(uuid)
        } catch (e: Exception) {
            logger.warn("Unable to delete file - ignoring", e)
        }
    }

    private fun getSegmentSize() = defaultSegmentSize

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.inputStreamToChunks(input: InputStream, chunkSize: Int) =
        produce {
            val bis = BufferedInputStream(input)
            val buffer = ByteArray(chunkSize)
            var bytesRead: Int
            do {
                bytesRead = readFully(bis, buffer)
                val chunk = ByteArray(bytesRead)
                buffer.copyInto(chunk, endIndex = bytesRead)
                send(chunk)
            } while (bytesRead == chunkSize)
        }

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var totalBytesRead = 0
        while (totalBytesRead < buffer.size) {
            val bytesRead = input.read(buffer, totalBytesRead, buffer.size - totalBytesRead)
            if (bytesRead == -1) break // End of stream reached
            totalBytesRead += bytesRead
        }
        return if (totalBytesRead > 0) totalBytesRead else -1
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun streamObject(dbVaultObject: DbVaultObject, outputStream: OutputStream): Unit =
        BufferedOutputStream(outputStream).use { bos ->
            IOUtils.write(dbVaultObject.data, bos)
            if (!dbVaultObject.segmented) return
            val parallelism = defaultSegmentFetchParallelism
            val id = dbVaultObject.id!!

            runBlocking {
                flow<ByteArray> {
                    var segmentIndex = 0
                    while (true) {
                        dbVaultObjectSegmentRepository.findByIdOrNull(DbVaultObjectSegment.ObjectSegmentId(id, segmentIndex++))
                            ?.also {
                                entityManager.detach(it)
                                emit(it.segmentData)
                            } ?: break
                    }
                }.buffer(parallelism).flowOn(dispatcher.limitedParallelism(parallelism))
                    .collect { data -> IOUtils.write(data, bos) }
            }
        }

    override fun streamObjectAsPipedInputStream(uuid: UUID, outputStream: OutputStream): InputStream {
        var pOut = retrieveObject(uuid, outputStream)
        return pipeOutputStreamToPipedInputStream {
            outputStream.buffered().use {}
        }
    }

    private fun pipeOutputStreamToPipedInputStream(writeToOutput: (OutputStream) -> Unit): PipedInputStream {
        val pipedInputStream = PipedInputStream()
        val pipedOutputStream = PipedOutputStream(pipedInputStream)

        // Schreibe in einem separaten Thread in den PipedOutputStream
        thread {
            pipedOutputStream.use { outputStream ->
                writeToOutput(outputStream)  // Daten werden in den OutputStream geschrieben
            }
        }

        return pipedInputStream
    }
}