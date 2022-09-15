package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO

class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var shouldReturnError = false

    override suspend fun getReminders(): com.udacity.project4.locationreminders.data.dto.Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return com.udacity.project4.locationreminders.data.dto.Result.Error("Test Exception")
        }
        reminders?.let {
            return com.udacity.project4.locationreminders.data.dto.Result.Success(ArrayList(it))
        }
        return com.udacity.project4.locationreminders.data.dto.Result.Error("No reminders found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): com.udacity.project4.locationreminders.data.dto.Result<ReminderDTO> {
        if (shouldReturnError)
            return com.udacity.project4.locationreminders.data.dto.Result.Error("Test Exception")
        val found = reminders?.first { it.id == id }
        return if (found != null)
            com.udacity.project4.locationreminders.data.dto.Result.Success(found)
        else
            com.udacity.project4.locationreminders.data.dto.Result.Error("Reminder $id not found")
    }
    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }
}