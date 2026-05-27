package com.sumryfen.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * STT (Speech-to-Text) API client.
 * Panggil Groq Whisper langsung dari Android via HTTP multipart.
 */
class SttApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val wavMediaType = "audio/wav".toMediaType()

    /**
     * Kirim audio WAV ke Groq Whisper untuk transkripsi.
     * @return Teks hasil transkripsi
     * @throws RateLimitException jika HTTP 429
     * @throws Exception untuk error lain (network, auth, server)
     */
    suspend fun transcribe(
        baseUrl: String,
        apiKey: String,
        model: String,
        audioWav: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trimEnd('/')
        val url = "$cleanUrl/audio/transcriptions"

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart(
                    "file",
                    "audio_${System.currentTimeMillis()}.wav",
                    audioWav.toRequestBody(wavMediaType)
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            when (response.code) {
                200 -> {
                    try {
                        val json = gson.fromJson(body, JsonObject::class.java)
                        json.get("text")?.asString ?: ""
                    } catch (e: Exception) {
                        throw Exception("Respons STT tidak valid.")
                    }
                }
                401 -> throw Exception("API Key STT tidak valid. Periksa Pengaturan.")
                429 -> {
                    val retryAfter = response.header("Retry-After", "5")?.toIntOrNull() ?: 5
                    throw RateLimitException(retryAfter)
                }
                500, 502, 503 -> throw Exception("Server STT sedang sibuk. Coba lagi.")
                else -> throw Exception("STT gagal (${response.code}). Periksa konfigurasi.")
            }
        } catch (e: SocketTimeoutException) {
            throw Exception("Koneksi timeout. Periksa internet Anda.")
        } catch (e: ConnectException) {
            throw Exception("Tidak dapat terhubung ke server STT. Periksa Base URL.")
        } catch (e: UnknownHostException) {
            throw Exception("Tidak ada koneksi internet.")
        } catch (e: RateLimitException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Gagal menghubungi STT: ${e.message}")
        }
    }
}
