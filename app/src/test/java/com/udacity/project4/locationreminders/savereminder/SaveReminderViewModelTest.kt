package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.data.source.FakeDataSource
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.pauseDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var dataSource: FakeDataSource

    @Before
    fun setupViewModel() {
        dataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            dataSource
        )
    }

    @Test
    fun saveReminder_test() = runBlocking {
        val reminder1 = ReminderDataItem(
            "R1 Title",
            "R1 Description",
            "R1 Location",
            30.310,
            31.320
        )
        viewModel.saveReminder(reminder1)

        TestCase.assertEquals("Reminder Saved !", viewModel.showToast.getOrAwaitValue())
    }

    @Test
    fun validateEnteredData_test_trueResult() = runBlocking {
        val reminder1 = ReminderDataItem(
            "R1 Title",
            "R1 Description",
            "R1 Location",
            30.310,
            31.320
        )
        val result = viewModel.validateEnteredData(reminder1)

        TestCase.assertTrue(result)
    }

    @Test
    fun validateEnteredData_test_emptyTitle_falseResult() = runBlocking {
        val reminder1 = ReminderDataItem(
            null ,
            "R1 Description",
            "R1 Location",
            30.310,
            31.320
        )
        val result = viewModel.validateEnteredData(reminder1)

        assertFalse(result)
    }

    @Test
    fun validateAndSaveReminder_test_trueResult() = runBlocking {
        val reminder1 = ReminderDataItem(
            "R1 Title",
            "R1 Description",
            "R1 Location",
            30.310,
            31.320
        )
        val result = viewModel.validateAndSaveReminder(reminder1)

        TestCase.assertTrue(result)
    }

    @Test
    fun validateAndSaveReminder_test_emptyTitle_falseResult() = runBlocking {
        val reminder1 = ReminderDataItem(
            null ,
            "R1 Description",
            "R1 Location",
            30.310,
            31.320
        )
        val result = viewModel.validateAndSaveReminder(reminder1)

        assertFalse(result)
    }

    @Test
    fun checkLoading() {

        mainCoroutineRule.pauseDispatcher()

        val reminder1 = ReminderDataItem(
            "",
            "R1 Description",
            "",
            30.310,
            31.320
        )
        viewModel.saveReminder(reminder1)
        TestCase.assertTrue(viewModel.showLoading.getOrAwaitValue())
    }

    @After
    fun teardown() {
        stopKoin()
    }

}