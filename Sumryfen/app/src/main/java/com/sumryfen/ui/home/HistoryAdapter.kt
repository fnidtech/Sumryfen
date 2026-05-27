package com.sumryfen.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.sumryfen.R
import com.sumryfen.data.local.MeetingEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (Long) -> Unit
) : ListAdapter<MeetingEntity, HistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: MaterialTextView = itemView.findViewById(R.id.tv_item_title)
        private val tvDate: MaterialTextView = itemView.findViewById(R.id.tv_item_date)
        private val tvDuration: MaterialTextView = itemView.findViewById(R.id.tv_item_duration)
        private val tvSummaryPreview: MaterialTextView = itemView.findViewById(R.id.tv_item_summary)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_item_delete)

        fun bind(meeting: MeetingEntity) {
            tvTitle.text = meeting.title
            tvDate.text = formatDate(meeting.createdAt)
            tvDuration.text = formatDuration(meeting.durationSeconds)
            tvSummaryPreview.text = meeting.summary.take(120).ifEmpty { "Tidak ada ringkasan" }

            itemView.setOnClickListener { onItemClick(meeting.id) }
            btnDelete.setOnClickListener {
                // TODO: Hapus meeting (akan ditambahkan di Fase 4)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MeetingEntity>() {
        override fun areItemsTheSame(old: MeetingEntity, new: MeetingEntity): Boolean =
            old.id == new.id

        override fun areContentsTheSame(old: MeetingEntity, new: MeetingEntity): Boolean =
            old == new
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id"))
    return sdf.format(Date(timestamp * 1000))
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "${h}j ${m}m" else "${m}m ${s}s"
}
