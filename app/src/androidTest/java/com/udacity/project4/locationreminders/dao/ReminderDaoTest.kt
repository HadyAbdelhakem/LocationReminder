package com.udacity.project4.locationreminders.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Database
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class ReminderDaoTest {
    private lateinit var database: RemindersDatabase
    private lateinit var dao : RemindersDao

    @get:Rule
    var instantExecuteRule = InstantTaskExecutorRule()

    @Before
    fun setup(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.reminderDao()
    }

    @Test
    fun insertReminderAndGetReminders() = runBlocking{

        val reminder = ReminderDTO(
            "R1 Title", "R1 Description", "R1 Location",
            30.310, 31.320
        )
        dao.saveReminder(reminder)

        val getReminder = dao.getReminderById(reminder.id)

        TestCase.assertTrue(reminder.id == getReminder?.id)
        TestCase.assertTrue(reminder.title == getReminder?.title)
        TestCase.assertTrue(reminder.description == getReminder?.description)
        TestCase.assertTrue(reminder.location == getReminder?.location)
    }

    @After
    fun toCloseDB(){
        database.close()
    }


}