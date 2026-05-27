package com.sumryfen.ui.meeting

data class MeetingState(
    val status: MeetingStatus = MeetingStatus.Idle,
    val transcript: String = "",
    val summary: String = "",
    val elapsedSeconds: Int = 0,
    val error: String? = null,
    val isTransientError: Boolean = false,
    val isOffline: Boolean = false
)

enum class MeetingStatus {
    Idle,
    Recording,
    Stopped,
    Error
}
