package com.sumryfen.data.remote

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * LLM (Large Language Model) API client.
 * Panggil Groq Llama langsung dari Android via HTTP JSON.
 */
class LlmApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Generate ringkasan dari transkrip yang terakumulasi.
     * @return Teks ringkasan
     * @throws RateLimitException jika HTTP 429
     * @throws Exception untuk error lain (network, auth, server)
     */
    suspend fun summarize(
        baseUrl: String,
        apiKey: String,
        model: String,
        transcript: String,
        previousSummary: String?
    ): String = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trimEnd('/')
        val url = "$cleanUrl/chat/completions"

        try {
            val systemPrompt = (
                "Anda adalah asisten yang membuat ringkasan meeting dalam bahasa Indonesia. " +
                "Buat ringkasan poin-poin penting secara terstruktur dan jelas. " +
                "Gunakan bahasa Indonesia yang baik dan benar."
            )

            var userMessage = ""
            if (previousSummary != null) {
                userMessage += "Ringkasan sebelumnya:\n$previousSummary\n\n"
            }
            userMessage += (
                "Transkrip meeting:\n$transcript\n\n" +
                "Buat ringkasan terbaru dari seluruh transkrip di atas. " +
                "Jika ada ringkasan sebelumnya, gabungkan dengan informasi baru. " +
                "Gunakan poin-poin (bullet points) dalam bahasa Indonesia. " +
                "Pisahkan setiap poin dengan baris baru."
            )

            val jsonBody = JsonObject().apply {
                addProperty("model", model)
                add("messages", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", systemPrompt)
                    })
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", userMessage)
                    })
                })
                addProperty("temperature", 0.3)
                addProperty("max_tokens", 1024)
            }

            val requestBody = gson.toJson(jsonBody).toRequestBody(jsonMediaType)

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
                        json.getAsJsonArray("choices")?.get(0)?.asJsonObject
                            ?.get("message")?.asJsonObject
                            ?.get("content")?.asString ?: ""
                    } catch (e: Exception) {
                        throw Exception("Respons LLM tidak valid.")
                    }
                }
                401 -> throw Exception("API Key LLM tidak valid. Periksa Pengaturan.")
                429 -> {
                    val retryAfter = response.header("Retry-After", "5")?.toIntOrNull() ?: 5
                    throw RateLimitException(retryAfter)
                }
                500, 502, 503 -> throw Exception("Server LLM sedang sibuk. Coba lagi.")
                else -> throw Exception("LLM gagal (${response.code}). Periksa konfigurasi.")
            }
        } catch (e: SocketTimeoutException) {
            throw Exception("Koneksi timeout. Periksa internet Anda.")
        } catch (e: ConnectException) {
            throw Exception("Tidak dapat terhubung ke server LLM. Periksa Base URL.")
        } catch (e: UnknownHostException) {
            throw Exception("Tidak ada koneksi internet.")
        } catch (e: RateLimitException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Gagal menghubungi LLM: ${e.message}")
        }
    }
}
