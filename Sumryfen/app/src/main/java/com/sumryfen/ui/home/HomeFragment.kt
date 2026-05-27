package com.sumryfen.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sumryfen.MainActivity
import com.sumryfen.R
import com.sumryfen.ui.detail.DetailFragment
import com.sumryfen.ui.meeting.MeetingFragment
import com.sumryfen.ui.settings.SettingsFragment
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvHistory = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_history)
        val fabNewMeeting = view.findViewById<FloatingActionButton>(R.id.fab_new_meeting)
        val btnSettings = view.findViewById<ImageButton>(R.id.btn_settings)

        adapter = HistoryAdapter { meetingId ->
            openDetail(meetingId)
        }

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter

        fabNewMeeting.setOnClickListener {
            (requireActivity() as MainActivity).openFragment(MeetingFragment())
        }

        btnSettings.setOnClickListener {
            (requireActivity() as MainActivity).openFragment(SettingsFragment())
        }

        // Credit link
        val tvCredit = view.findViewById<TextView>(R.id.tv_credit)
        tvCredit.setOnClickListener {
            val url = getString(R.string.credit_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Observe meetings list
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.meetings.collect { meetings ->
                    adapter.submitList(meetings)
                }
            }
        }
    }

    private fun openDetail(meetingId: Long) {
        val fragment = DetailFragment.newInstance(meetingId)
        (requireActivity() as MainActivity).openFragment(fragment)
    }
}
