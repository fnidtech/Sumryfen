package com.sumryfen.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sumryfen.data.local.AppDatabase
import com.sumryfen.data.local.MeetingEntity
import com.sumryfen.data.repository.MeetingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MeetingRepository(
        AppDatabase.getInstance(application).meetingDao()
    )

    val meetings: StateFlow<List<MeetingEntity>> = repository.getAllMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
