package com.sumryfen.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sumryfen.data.local.AppDatabase
import com.sumryfen.data.local.MeetingEntity
import com.sumryfen.data.repository.MeetingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailState(
    val meeting: MeetingEntity? = null,
    val isLoading: Boolean = true
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MeetingRepository(
        AppDatabase.getInstance(application).meetingDao()
    )

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    fun loadMeeting(meetingId: Long) {
        viewModelScope.launch {
            _state.value = DetailState(isLoading = true)
            val meeting = repository.getMeetingById(meetingId)
            _state.value = DetailState(meeting = meeting, isLoading = false)
        }
    }
}
