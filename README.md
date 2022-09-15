# Location-Reminder

This is a TODO list app with location reminders that reminds the user to do something when the user is at a specific location. 
The app requires the user to create an account and login to set and access reminders.

# Description

* Login screen to ask users to login using an email address or a Google account. Upon successful login, navigate the user to the Reminders screen. If there is no account, the app    will navigate to a Register screen.
* Register screen to allow a user to register using an email address or a Google account.
* A screen that displays the reminders retrieved from local storage. If there are no reminders, display a "No Data" indicator. If there are any errors, display an error message.
* A screen that shows a map with the user's current location and asks the user to select a point of interest to create a reminder.
* A screen to add a reminder when a user reaches the selected location. Each reminder will include
    title
    description
    selected location
* Reminder data is saved to local storage.
* For each reminder, a geofencing request is created in the background that fires up a notification when the user enters the geofencing area.
* Testing is done for the ViewModels, Coroutines and LiveData objects.
* A FakeDataSource to replace the Data Layer and test the app in isolation.
* Used Espresso and Mockito to test each screen of the app.
* Test DAO (Data Access Object) and Repository classes.
* Testing for the error messages.
* End-To-End testing for the Fragments navigation.
