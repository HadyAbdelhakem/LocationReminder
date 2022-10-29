package com.udacity.project4.locationreminders.data

import com.udacity.project4.data.source.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.IsEqual
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DefaultReminderRepositoryTest {

    private val reminder1  = ReminderDTO("R1 Title" , "R1 Description" , "R1 Location" ,
        30.30 , 30.30)
    private val reminder2  = ReminderDTO("R2 Title" , "R2 Description" , "R2 Location" ,
        30.30 , 30.30)
    private val reminder3  = ReminderDTO("R3 Title" , "R3 Description" , "R3 Location" ,
        30.30 , 30.30)

    private val localReminders: List<ReminderDTO> = listOf(reminder1, reminder2).sortedBy { it.id }
    private val newReminder: List<ReminderDTO> = listOf(reminder3).sortedBy { it.id }

    private lateinit var remindersLocalDataSource : FakeDataSource

    private lateinit var remindersRepository : DefaultReminderRepository

    @Before
    fun createRepository() {
        remindersLocalDataSource = FakeDataSource(localReminders.toMutableList())

        remindersRepository = DefaultReminderRepository(
            remindersLocalDataSource , Dispatchers.Unconfined
        )
    }

    @Test
    fun getReminders() = runBlocking {
        val reminders = remindersRepository.getReminders() as Success

        assertThat(reminders.data , IsEqual(localReminders))
    }
}