package fr.plopez.go4lunch.view.model

data class WorkmateWithSelectedRestaurant(
    val workmateName: String = "",
    val workmateEmail: String = "",
    val workmatePhotoUrl: String = "",
    val selectedRestaurantName: String = "",
    val selectedRestaurantId: String = ""
)