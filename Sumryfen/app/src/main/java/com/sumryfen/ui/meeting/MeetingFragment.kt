package com.sumryfen.ui.meeting

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sumryfen.MainActivity
import com.sumryfen.R
import com.sumryfen.ui.home.HomeFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MeetingFragment : Fragment() {

    private val viewModel: MeetingViewModel by viewModels()

    private var btnStop: MaterialButton? = null
    private var tvTimer: TextView? = null
    private var tvTranscript: TextView? = null
    private var tvSummary: TextView? = null
    private var tvOfflineBanner: TextView? = null
    private var currentSnackbar: Snackbar? = null
    private var hideErrorJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meeting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnStop = view.findViewById(R.id.btn_stop)
        tvTimer = view.findViewById(R.id.tv_timer)
        tvTranscript = view.findViewById(R.id.tv_transcript)
        tvSummary = view.findViewById(R.id.tv_summary)
        tvOfflineBanner = view.findViewById(R.id.tv_offline_banner)

        btnStop?.setOnClickListener {
            viewModel.stopMeeting()
        }

        // Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateUi(state)
                }
            }
        }

        // Validasi konfigurasi sebelum mulai
        val configError = viewModel.validateConfig()
        if (configError != null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Konfigurasi Belum Lengkap")
                .setMessage(configError)
                .setPositiveButton("Kembali") { _, _ ->
                    (requireActivity() as MainActivity).supportFragmentManager.popBackStack()
                }
                .setCancelable(false)
                .show()
            return
        }

        // Request permission and start
        if (hasAudioPermission()) {
            viewModel.startMeeting()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.startMeeting()
            } else {
                Snackbar.make(
                    requireView(),
                    "Izin mikrofon diperlukan untuk merekam",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateUi(state: MeetingState) {
        tvTimer?.text = formatTime(state.elapsedSeconds)
        tvTranscript?.text = state.transcript.ifEmpty { "Menunggu transkrip..." }
        tvSummary?.text = state.summary.ifEmpty { "Menunggu ringkasan..." }

        // Offline indicator
        tvOfflineBanner?.visibility =
            if (state.isOffline && state.status == MeetingStatus.Recording) View.VISIBLE
            else View.GONE

        // Auto-scroll transcript
        tvTranscript?.post {
            tvTranscript?.let {
                val layout = it.layout
                if (layout != null) {
                    val scrollAmount = layout.getLineTop(it.lineCount) - it.height
                    if (scrollAmount > 0) {
                        it.scrollTo(0, scrollAmount)
                    }
                }
            }
        }

        // Button state
        when (state.status) {
            MeetingStatus.Recording -> {
                btnStop?.isEnabled = true
                btnStop?.text = "Stop"
            }
            MeetingStatus.Stopped -> {
                btnStop?.isEnabled = false
                btnStop?.text = "Selesai"
                showStoppedMessage()
            }
            MeetingStatus.Error -> {
                btnStop?.isEnabled = true
                btnStop?.text = "Stop"
                showDialogError(state.error ?: "Terjadi kesalahan")
            }
            else -> {
                btnStop?.isEnabled = false
                btnStop?.text = "Mulai"
            }
        }

        // Transient error via Snackbar
        if (state.error != null && state.status != MeetingStatus.Error) {
            showTransientError(state.error)
        }
    }

    private fun showTransientError(message: String) {
        currentSnackbar?.dismiss()
        currentSnackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        currentSnackbar?.show()

        // Hapus error dari state setelah ditampilkan
        hideErrorJob?.cancel()
        hideErrorJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)
            viewModel.clearError()
        }
    }

    private fun showDialogError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Terjadi Kesalahan")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                viewModel.stopMeeting()
            }
            .setCancelable(false)
            .show()
    }

    private fun showStoppedMessage() {
        Snackbar.make(
            requireView(),
            "Meeting selesai. Transkrip dan ringkasan telah disimpan.",
            Snackbar.LENGTH_LONG
        ).show()

        // Kembali ke Home setelah beberapa detik
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            if (isAdded) {
                (requireActivity() as MainActivity).openFragment(HomeFragment())
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentSnackbar?.dismiss()
        hideErrorJob?.cancel()
        btnStop = null
        tvTimer = null
        tvTranscript = null
        tvSummary = null
        tvOfflineBanner = null
    }

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 1001
    }
}
