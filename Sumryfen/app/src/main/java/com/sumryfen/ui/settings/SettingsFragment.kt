package com.sumryfen.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.sumryfen.R
import com.sumryfen.data.repository.SettingsRepository

class SettingsFragment : Fragment() {

    private lateinit var settingsRepo: SettingsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsRepo = SettingsRepository(requireContext())

        val switchSaveAudio = view.findViewById<SwitchMaterial>(R.id.switch_save_audio)
        val etSttBaseUrl = view.findViewById<TextInputEditText>(R.id.et_stt_base_url)
        val etSttApiKey = view.findViewById<TextInputEditText>(R.id.et_stt_api_key)
        val etSttModel = view.findViewById<TextInputEditText>(R.id.et_stt_model)
        val etLlmBaseUrl = view.findViewById<TextInputEditText>(R.id.et_llm_base_url)
        val etLlmApiKey = view.findViewById<TextInputEditText>(R.id.et_llm_api_key)
        val etLlmModel = view.findViewById<TextInputEditText>(R.id.et_llm_model)

        // Load saved values
        switchSaveAudio.isChecked = settingsRepo.isSaveAudioEnabled()
        etSttBaseUrl.setText(settingsRepo.getSttBaseUrl())
        etSttApiKey.setText(settingsRepo.getSttApiKey())
        etSttModel.setText(settingsRepo.getSttModel())
        etLlmBaseUrl.setText(settingsRepo.getLlmBaseUrl())
        etLlmApiKey.setText(settingsRepo.getLlmApiKey())
        etLlmModel.setText(settingsRepo.getLlmModel())

        // Save on change
        switchSaveAudio.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setSaveAudioEnabled(isChecked)
        }
        etSttBaseUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) settingsRepo.setSttBaseUrl(etSttBaseUrl.text?.toString() ?: "")
        }
        etSttApiKey.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) settingsRepo.setSttApiKey(etSttApiKey.text?.toString() ?: "")
        }
        etSttModel.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) settingsRepo.setSttModel(etSttModel.text?.toString() ?: "")
        }
        etLlmBaseUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) settingsRepo.setLlmBaseUrl(etLlmBaseUrl.text?.toString() ?: "")
        }
        etLlmApiKey.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) settingsRepo.setLlmApiKey(etLlmApiKey.text?.toString() ?: "")
        }
        etLlmModel.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) settingsRepo.setLlmModel(etLlmModel.text?.toString() ?: "")
        }
    }
}
