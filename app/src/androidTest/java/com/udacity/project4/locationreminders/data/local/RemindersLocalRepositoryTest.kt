package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainAndroidTestCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var mainCoroutineRule = MainAndroidTestCoroutineRule()
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase
    private lateinit var remindersDAO: RemindersDao
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        )
                .allowMainThreadQueries()
                .build()
        remindersDAO = database.reminderDao()
        repository =
                RemindersLocalRepository(
                        remindersDAO,
                        Dispatchers.Main
                )
    }

    @After
    fun closeDB() {
        database.close()
    }

    @Test
    fun saveReminderAndGetByID() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDTO(
                title = "Basketball",
                description = "Don't get crossed up or dunked on!",
                location = "B-Ball Court",
                latitude = 75.1234,
                longitude = 3333.1234
        )
        repository.saveReminder(reminder)
        val reminderLoaded = repository.getReminder(reminder.id) as com.udacity.project4.locationreminders.data.dto.Result.Success<ReminderDTO>
        val loaded = reminderLoaded.data

        assertThat(loaded, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun deleteAllRemindersAndReminders() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDTO(
                title = "Basketball",
                description = "Don't get crossed up or dunked on!",
                location = "B-Ball Court",
                latitude = 75.1234,
                longitude = 3333.1234
        )
        repository.saveReminder(reminder)
        repository.deleteAllReminders()
        val reminders = repository.getReminders() as com.udacity.project4.locationreminders.data.dto.Result.Success<List<ReminderDTO>>
        val data = reminders.data
        assertThat(data.isEmpty(), `is`(true))

    }

    @Test
    fun noRemindersFoundGetReminderById() = mainCoroutineRule.runBlockingTest {
        val reminder = repository.getReminder("3") as com.udacity.project4.locationreminders.data.dto.Result.Error
        assertThat(reminder.message, notNullValue())
        assertThat(reminder.message, `is`("Reminder not found!"))
    }
}