package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {

    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var appContext: Application

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /*private val dataBindingIdlingResource = DataBindingIdlingResource()*/

    @Before
    fun setup(){

        stopKoin()
        appContext = ApplicationProvider.getApplicationContext()

        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

        startKoin {
            modules(listOf(myModule))
        }

        reminderDataSource = get()

        runBlocking {
            reminderDataSource.deleteAllReminders()
        }

//        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @Test
    fun reminderListFragment(){

        runBlocking {

            //GIVEN
            val reminder = ReminderDTO(
                "R1 Title", "R1 Description", "R1 Location",
                30.310, 31.320
            )

            reminderDataSource.saveReminder(reminder)

            //WHEN
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

            //THEN
            Espresso.onView(ViewMatchers.withText(reminder.title))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withText(reminder.description))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withText(reminder.location))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

            Thread.sleep(2000)

        }

    }

    @Test
    fun navigateToSaveReminder() {

        //GIVEN
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        //WHEN
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())

        //THEN
        Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())

    }

    @After
    fun unregisterIdlingResourceAndStopKoin() {
//        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
        stopKoin()
    }
}