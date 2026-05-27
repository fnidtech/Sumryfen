package com.sumryfen.data.repository

import com.sumryfen.data.local.MeetingDao
import com.sumryfen.data.local.MeetingEntity
import kotlinx.coroutines.flow.Flow

class MeetingRepository(private val meetingDao: MeetingDao) {

    fun getAllMeetings(): Flow<List<MeetingEntity>> = meetingDao.getAllMeetings()

    suspend fun getMeetingById(id: Long): MeetingEntity? = meetingDao.getMeetingById(id)

    suspend fun insertMeeting(meeting: MeetingEntity): Long = meetingDao.insertMeeting(meeting)

    suspend fun updateMeeting(meeting: MeetingEntity) = meetingDao.updateMeeting(meeting)

    suspend fun deleteMeeting(meeting: MeetingEntity) = meetingDao.deleteMeeting(meeting)
}
