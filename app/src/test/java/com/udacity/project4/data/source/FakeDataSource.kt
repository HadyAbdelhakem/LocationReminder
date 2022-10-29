package com.udacity.project4.data.source

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error


class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf())
    : ReminderDataSource {
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        reminders?.let { return Success(ArrayList(it)) }
        return Error(
            "Reminders not found"
        )
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }
}