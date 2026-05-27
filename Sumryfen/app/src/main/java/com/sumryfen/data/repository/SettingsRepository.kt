package com.sumryfen.data.repository

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sumryfen_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVE_AUDIO = "save_audio"
        private const val KEY_STT_BASE_URL = "stt_base_url"
        private const val KEY_STT_API_KEY = "stt_api_key"
        private const val KEY_STT_MODEL = "stt_model"
        private const val KEY_LLM_BASE_URL = "llm_base_url"
        private const val KEY_LLM_API_KEY = "llm_api_key"
        private const val KEY_LLM_MODEL = "llm_model"

        val DEFAULT_STT_BASE_URL = "https://api.groq.com/openai/v1"
        val DEFAULT_STT_MODEL = "whisper-large-v3-turbo"
        val DEFAULT_LLM_BASE_URL = "https://api.groq.com/openai/v1"
        val DEFAULT_LLM_MODEL = "llama-3-8b-instant"
    }

    fun isSaveAudioEnabled(): Boolean = prefs.getBoolean(KEY_SAVE_AUDIO, false)
    fun setSaveAudioEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SAVE_AUDIO, enabled).apply()

    fun getSttBaseUrl(): String = prefs.getString(KEY_STT_BASE_URL, DEFAULT_STT_BASE_URL) ?: DEFAULT_STT_BASE_URL
    fun setSttBaseUrl(url: String) = prefs.edit().putString(KEY_STT_BASE_URL, url).apply()

    fun getSttApiKey(): String = prefs.getString(KEY_STT_API_KEY, "") ?: ""
    fun setSttApiKey(key: String) = prefs.edit().putString(KEY_STT_API_KEY, key).apply()

    fun getSttModel(): String = prefs.getString(KEY_STT_MODEL, DEFAULT_STT_MODEL) ?: DEFAULT_STT_MODEL
    fun setSttModel(model: String) = prefs.edit().putString(KEY_STT_MODEL, model).apply()

    fun getLlmBaseUrl(): String = prefs.getString(KEY_LLM_BASE_URL, DEFAULT_LLM_BASE_URL) ?: DEFAULT_LLM_BASE_URL
    fun setLlmBaseUrl(url: String) = prefs.edit().putString(KEY_LLM_BASE_URL, url).apply()

    fun getLlmApiKey(): String = prefs.getString(KEY_LLM_API_KEY, "") ?: ""
    fun setLlmApiKey(key: String) = prefs.edit().putString(KEY_LLM_API_KEY, key).apply()

    fun getLlmModel(): String = prefs.getString(KEY_LLM_MODEL, DEFAULT_LLM_MODEL) ?: DEFAULT_LLM_MODEL
    fun setLlmModel(model: String) = prefs.edit().putString(KEY_LLM_MODEL, model).apply()
}
