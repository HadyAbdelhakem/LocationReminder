package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.*

class DefaultReminderRepository constructor(
    private val remindersLocalDataSource: ReminderDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RemindersRepo {
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return remindersLocalDataSource.getReminders()
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        coroutineScope {
            launch { remindersLocalDataSource.saveReminder(reminder) }
        }
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return remindersLocalDataSource.getReminder(id)
    }

    override suspend fun deleteAllReminders() {
        withContext(ioDispatcher) {
            coroutineScope {
                launch { remindersLocalDataSource.deleteAllReminders() }
            }
        }
    }
}