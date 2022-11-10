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
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if (shouldReturnError){
            return Error("Test exception")
        }

        reminders?.let { return Success(ArrayList(it)) }

        return Success(listOf())

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        if(shouldReturnError){
            return Error("Test exception")
        }

        reminders?.firstOrNull() { it->it.id == id }?.let {
            return Success(it)
        }

        return Error("Reminder not found!")

    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }
}