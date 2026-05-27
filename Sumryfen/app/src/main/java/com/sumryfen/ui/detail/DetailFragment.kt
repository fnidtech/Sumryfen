package com.sumryfen.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.sumryfen.R
import kotlinx.coroutines.launch

class DetailFragment : Fragment() {

    private val viewModel: DetailViewModel by viewModels()

    private var tvTitle: TextView? = null
    private var tvTranscript: TextView? = null
    private var tvSummary: TextView? = null
    private var btnShare: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tv_detail_title)
        tvSummary = view.findViewById(R.id.tv_detail_summary)
        tvTranscript = view.findViewById(R.id.tv_detail_transcript)
        btnShare = view.findViewById(R.id.btn_share)

        btnShare?.setOnClickListener {
            shareMeeting()
        }

        // Load meeting
        val meetingId = arguments?.getLong(ARG_MEETING_ID, -1) ?: -1
        if (meetingId > 0) {
            viewModel.loadMeeting(meetingId)
        }

        // Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (!state.isLoading && state.meeting != null) {
                        val m = state.meeting
                        tvTitle?.text = m.title
                        tvSummary?.text = m.summary.ifEmpty { "Tidak ada ringkasan" }
                        tvTranscript?.text = m.transcript.ifEmpty { "Tidak ada transkrip" }
                    }
                }
            }
        }
    }

    private fun shareMeeting() {
        val state = viewModel.state.value
        val meeting = state.meeting ?: return

        val text = """
            ${meeting.title}
            
            Ringkasan:
            ${meeting.summary}
            
            Transkrip:
            ${meeting.transcript}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Bagikan Meeting"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tvTitle = null
        tvTranscript = null
        tvSummary = null
        btnShare = null
    }

    companion object {
        private const val ARG_MEETING_ID = "meeting_id"

        fun newInstance(meetingId: Long): DetailFragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_MEETING_ID, meetingId)
                }
            }
        }
    }
}
