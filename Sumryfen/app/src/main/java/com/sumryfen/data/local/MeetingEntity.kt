package com.sumryfen.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val transcript: String = "",

    val summary: String = "",

    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() / 1000,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int = 0
)
