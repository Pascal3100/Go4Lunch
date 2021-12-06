package fr.plopez.go4lunch.view.main_activity.list_workmates

data class WorkmateWithSelectedRestaurant(
    val workmateName: String = "",
    val workmateEmail: String = "",
    val workmatePhotoUrl: String = "",
    val selectedRestaurantName: String = "",
    val selectedRestaurantId: String = ""
)