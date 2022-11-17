package com.udacity.project4.data.source

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error


class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf())
    : ReminderDataSource {

    private var shouldReturnError = false

    fun setReturnError (value: Boolean) {
        if (value == true) {
            shouldReturnError = true
        } else {
            shouldReturnError = false
        }
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if (shouldReturnError){
            return Error("Test exception")
        }
        return Success(ArrayList(reminders as ArrayList<ReminderDTO>))

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        if(shouldReturnError){
            return Error("Test exception")
        }

        val reminder = reminders?.find { it.id == id }
        return if(reminder != null){
            Success(reminder)
        }else {
            Error("Did not find Reminder")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }
}

