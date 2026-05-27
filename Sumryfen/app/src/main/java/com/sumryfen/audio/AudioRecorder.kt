package com.sumryfen.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val bufferSize: Int by lazy {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        minBuf * BUFFER_SIZE_FACTOR
    }

    /**
     * Mulai rekaman. Mengeluarkan Flow<ByteArray> — setiap item adalah chunk PCM mentah.
     */
    fun start(): Flow<ByteArray> = callbackFlow {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            close(RuntimeException("Gagal inisialisasi AudioRecord"))
            return@callbackFlow
        }

        audioRecord?.startRecording()
        isRecording = true
        val buffer = ByteArray(bufferSize)

        while (isRecording) {
            val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            if (bytesRead > 0) {
                val chunk = buffer.copyOf(bytesRead)
                trySend(chunk)
            }
        }

        awaitClose { stop() }
    }

    fun stop() {
        isRecording = false
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null
    }

    fun isRecording(): Boolean = isRecording

    /**
     * Gabungkan beberapa chunk PCM menjadi satu array.
     */
    fun combinePcmChunks(chunks: List<ByteArray>): ByteArray {
        val totalSize = chunks.sumOf { it.size }
        val combined = ByteArray(totalSize)
        var offset = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, combined, offset, chunk.size)
            offset += chunk.size
        }
        return combined
    }

    /**
     * Encode raw PCM byte array to WAV format with proper header.
     */
    fun encodeToWav(pcmData: ByteArray): ByteArray {
        val dataSize = pcmData.size
        val sampleRate = SAMPLE_RATE.toLong()
        val bitsPerSample = 16
        val channels = 1
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val headerSize = 44
        val totalSize = headerSize + dataSize

        val wav = ByteArray(totalSize)
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize - 8)  // Chunk size (file size - 8)
        buffer.put("WAVE".toByteArray())

        // fmt sub-chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)              // Sub-chunk size (16 for PCM)
        buffer.putShort(1)             // Audio format (1 = PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate.toInt())
        buffer.putInt(byteRate.toInt())
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        // data sub-chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        buffer.put(pcmData)

        return wav
    }

    /**
     * Build full WAV file from accumulated chunks and save to disk.
     */
    suspend fun saveWavFile(chunks: List<ByteArray>, file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val allPcm = combinePcmChunks(chunks)
            val wavData = encodeToWav(allPcm)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(wavData) }
            true
        } catch (e: Exception) {
            false
        }
    }
}
