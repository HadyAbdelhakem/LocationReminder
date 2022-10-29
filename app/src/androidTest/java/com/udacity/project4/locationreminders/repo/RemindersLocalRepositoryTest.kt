package com.udacity.project4.locationreminders.repo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersLocalRepository = RemindersLocalRepository(
            database.reminderDao(), Dispatchers.Unconfined
        )
    }

    @Test
    fun getReminders() = runBlocking {

        val reminder = ReminderDTO(
            "R1 Title", "R1 Description", "R1 Location",
            30.310, 31.320
        )

        remindersLocalRepository.saveReminder(reminder)

        val reminders = remindersLocalRepository.getReminders()

        TestCase.assertTrue(reminders is Result.Success)

        reminders as Result.Success
        TestCase.assertTrue(reminders.data.size ==1)
    }

    @Test
    fun getRemindersTest_withEmptyList_returnTrue() = runBlocking {

        val reminders = remindersLocalRepository.getReminders()

        TestCase.assertTrue ( reminders is Result.Success )

        reminders as Result.Success
        TestCase.assertEquals(emptyList<ReminderDTO>(), reminders.data)
    }

    @Test
    fun saveReminder() = runBlocking {
        val reminder = ReminderDTO(
            "R1 Title", "R1 Description", "R1 Location",
            30.310, 31.320
        )
        remindersLocalRepository.saveReminder(reminder)

        val getReminder = remindersLocalRepository.getReminder(reminder.id)

        TestCase.assertTrue ( getReminder is Result.Success )

        getReminder as Result.Success

        TestCase.assertEquals(getReminder.data.title , "R1 Title")
        TestCase.assertEquals(getReminder.data.description , "R1 Description")
        TestCase.assertEquals(getReminder.data.location , "R1 Location")
    }

    @Test
    fun deleteAllRemindersTest() = runBlocking {
        val reminder = ReminderDTO(
            "R1 Title", "R1 Description", "R1 Location",
            30.310, 31.320
        )
        remindersLocalRepository.saveReminder(reminder)
        remindersLocalRepository.deleteAllReminders()

        val getReminder = remindersLocalRepository.getReminder(reminder.id)

        TestCase.assertTrue (getReminder is Result.Error)
        getReminder as Result.Error
        TestCase.assertTrue (getReminder.message == "Reminder not found!")
    }

    @After
    fun toCloseDB(){
        database.close()
    }

}