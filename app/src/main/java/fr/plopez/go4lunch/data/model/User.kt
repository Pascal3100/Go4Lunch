package fr.plopez.go4lunch.data.model

data class User(
    var logged: Boolean = false,
    var permissionsAccepted: Boolean = false,
    var name: String = "",
    var email: String = "",
    var photoURL: String = "",
    var selectedRestaurant: String = "",
    var isRestaurantSelected: Boolean = false
)